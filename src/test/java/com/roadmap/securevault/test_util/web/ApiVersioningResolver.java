package com.roadmap.securevault.test_util.web;

import com.roadmap.securevault.common.annotation.ApiVersion;
import com.roadmap.securevault.common.annotation.NoApiVersion;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.util.Arrays;
import java.util.Objects;

public class ApiVersioningResolver {
    public static String resolve(Class<?> controllerClass, String methodName, String path) {
        String version = Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .map(m -> {
                    if (AnnotatedElementUtils.hasAnnotation(m, NoApiVersion.class)) return null;

                    ApiVersion v = AnnotatedElementUtils.findMergedAnnotation(m, ApiVersion.class);
                    if (v != null) return v.value();

                    ApiVersion classV = AnnotatedElementUtils.findMergedAnnotation(controllerClass, ApiVersion.class);
                    return classV != null ? classV.value() : null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return version != null ? "/api/" + version + path : path;
    }
}
