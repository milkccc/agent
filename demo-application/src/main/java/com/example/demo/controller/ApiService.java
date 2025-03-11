package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class ApiService {
    private final Class<?> controllerClass = com.example.demo.controller.TestController.class;

    public void getApiOnStartup() {
        try {
            JSONArray apiInfoArray = fetchApiData();
            // 格式化输出JSON
            System.out.println(JSON.toJSONString(apiInfoArray, true));
        } catch (Exception e) {
            System.err.println("获取API失败: " + e.getMessage());
        }
    }

    public JSONArray getApiAfterStartup() {
        try {
            return fetchApiData();
        } catch (Exception e) {
            System.err.println("获取API失败: " + e.getMessage());
            return new JSONArray();
        }
    }

    private JSONArray fetchApiData() {
        JSONArray apiArray = new JSONArray();

        // 获取类级别的RequestMapping注解
        RequestMapping classMapping = AnnotationUtils.findAnnotation(controllerClass, RequestMapping.class);
        String baseUrl = "";
        if (classMapping != null && classMapping.value().length > 0) {
            baseUrl = classMapping.value()[0];
        }

        // 获取所有方法
        Method[] methods = controllerClass.getDeclaredMethods();
        for (Method method : methods) {
            JSONObject apiInfo = new JSONObject();
            apiInfo.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
            apiInfo.put("className", controllerClass.getName());
            apiInfo.put("methodName", method.getName());

            // 处理URL和HTTP方法
            String url = baseUrl;
            String httpMethod = "";

            // 处理GET请求
            GetMapping getMapping = AnnotationUtils.findAnnotation(method, GetMapping.class);
            if (getMapping != null && getMapping.value().length > 0) {
                url += getMapping.value()[0];
                httpMethod = "GET";
            }

            // 处理POST请求
            PostMapping postMapping = AnnotationUtils.findAnnotation(method, PostMapping.class);
            if (postMapping != null && postMapping.value().length > 0) {
                url += postMapping.value()[0];
                httpMethod = "POST";
            }

            apiInfo.put("url", url);
            apiInfo.put("httpMethod", httpMethod);

            // 处理参数信息
            JSONArray parameters = new JSONArray();
            for (Parameter param : method.getParameters()) {
                JSONObject paramInfo = new JSONObject();
                paramInfo.put("name", param.getName());
                paramInfo.put("type", param.getType().getSimpleName());

                if (param.getAnnotation(RequestParam.class) != null) {
                    paramInfo.put("annotationType", "RequestParam");
                } else if (param.getAnnotation(RequestBody.class) != null) {
                    paramInfo.put("annotationType", "RequestBody");
                }

                parameters.add(paramInfo);
            }
            apiInfo.put("parameters", parameters);

            apiArray.add(apiInfo);
        }

        return apiArray;
    }
}

