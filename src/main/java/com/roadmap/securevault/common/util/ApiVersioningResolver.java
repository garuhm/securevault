package com.roadmap.securevault.common.util;

import com.roadmap.securevault.common.annotation.ApiVersion;
import com.roadmap.securevault.common.annotation.NoApiVersion;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

public class ApiVersioningResolver {
    public static String resolve(Class<?> controllerClass, String methodName, String path) {
        Method method = findDeclaringMethod(controllerClass, methodName);

        if (method != null) {
            if (AnnotatedElementUtils.hasAnnotation(method, NoApiVersion.class)) {
                return path; // explicit opt-out wins, regardless of class-level annotations
            }
            ApiVersion methodVer = AnnotatedElementUtils.findMergedAnnotation(method, ApiVersion.class);
            if (methodVer != null) {
                return "/api/" + methodVer.value() + path;
            }
        }

        // no method-level decision -> use the ORIGINAL class's class-level annotation
        ApiVersion classVer = AnnotatedElementUtils.findMergedAnnotation(controllerClass, ApiVersion.class);
        return classVer != null ? "/api/" + classVer.value() + path : path;
    }

    private static Method findDeclaringMethod(Class<?> controllerClass, String methodName) {
        return Stream
                .<Class<?>>
                        iterate(
                                controllerClass,
                                class_ -> class_ != null && class_ != Object.class,
                                Class::getSuperclass)
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElse(null);
    }
}