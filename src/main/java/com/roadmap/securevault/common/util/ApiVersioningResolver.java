package com.roadmap.securevault.common.util;

import com.roadmap.securevault.common.annotation.ApiVersion;
import com.roadmap.securevault.common.annotation.NoApiVersion;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.util.Arrays;
import java.util.Objects;

public class ApiVersioningResolver {
    public static String resolve(Class<?> controllerClass, String methodName, String path) {
        String version = findInHierarchy(controllerClass, methodName);
        return version != null ? "/api/" + version + path : path;
    }

    private static String findInHierarchy(Class<?> controllerClass, String methodName) {
        if (controllerClass == null || controllerClass == Object.class) return null;

        String version = Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .map(method -> {
                    if (AnnotatedElementUtils.hasAnnotation(method, NoApiVersion.class)) return null;

                    ApiVersion methodVer = AnnotatedElementUtils.findMergedAnnotation(method, ApiVersion.class);
                    if (methodVer != null) return methodVer.value();

                    ApiVersion classVer = AnnotatedElementUtils.findMergedAnnotation(controllerClass, ApiVersion.class);
                    return classVer != null ? classVer.value() : null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (version != null) return version;

        // check class-level annotation before walking up
        ApiVersion classVer = AnnotatedElementUtils.findMergedAnnotation(controllerClass, ApiVersion.class);
        if (classVer != null) return classVer.value();

        return findInHierarchy(controllerClass.getSuperclass(), methodName);
    }
}
