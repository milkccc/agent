package com.example.agent.transformer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.agent.collector.ApiInfoCollector;
import javassist.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SpringControllerTransformer implements ClassFileTransformer {
    private static final String OUTPUT_DIR = "D:/api-logs/";
    private static final ConcurrentHashMap<String, List<JSONObject>> apiLogs = new ConcurrentHashMap<>();
    private final ClassPool classPool;

    public SpringControllerTransformer() {
        this.classPool = ClassPool.getDefault();
        // 创建输出目录
        new File(OUTPUT_DIR).mkdirs();
    }


    public static void saveToJson(String methodName, Object[] args) {
        try {
            JSONObject log = new JSONObject();
            log.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
            log.put("method", methodName);

            // 处理参数
            JSONObject params = new JSONObject();
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    params.put("param" + i, String.valueOf(args[i]));
                }
            }
            log.put("parameters", params);

            // 保存到内存
            apiLogs.computeIfAbsent(methodName, k -> new ArrayList<>()).add(log);

            // 写入文件
            String fileName = OUTPUT_DIR + "api_logs_" +
                    new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".json";

            synchronized (SpringControllerTransformer.class) {
                try (FileWriter writer = new FileWriter(fileName)) {
                    writer.write(JSON.toJSONString(apiLogs, true));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!className.contains("com/example/demo/controller")) {
            return null;
        }

        try {
            if (loader != null) {
                classPool.insertClassPath(new LoaderClassPath(loader));
            }

            classPool.insertClassPath(new ByteArrayClassPath(className, classfileBuffer));
            CtClass cc = classPool.get(className.replace('/', '.'));

            if (cc.isFrozen()) {
                cc.defrost();
            }

            boolean isModified = false;
            for (CtMethod method : cc.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    enhanceMethod(method);
                    isModified = true;
                }
            }

            return isModified ? cc.toBytecode() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void enhanceMethod(CtMethod method) throws CannotCompileException {
        StringBuilder code = new StringBuilder();
        code.append("{")
                .append("  com.example.agent.transformer.SpringControllerTransformer")
                .append(".saveToJson(\"")
                .append(method.getLongName())
                .append("\", $args);")
                .append("}");

        method.insertBefore(code.toString());
    }
}
