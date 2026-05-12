package com.roadmap.securevault.controller.helper;

import com.roadmap.securevault.controller.annotation.ApiVersion;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Optional;

public class VersionedRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo mapping = super.getMappingForMethod(method, handlerType);
        if (mapping == null) return null;

        ApiVersion version = findApiVersion(method, handlerType);
        if (version == null) return mapping;

        String prefix = "/api/" + version.value();
        RequestMappingInfo versionInfo =
                RequestMappingInfo
                        .paths(prefix)
                        .options(getBuilderConfiguration())
                        .build();
        return versionInfo.combine(mapping);
    }

    private ApiVersion findApiVersion(Method method, Class<?> handlerType) {
        return Optional
                .ofNullable(
                        AnnotatedElementUtils
                                .findMergedAnnotation(method, ApiVersion.class))
                .orElseGet(() ->
                        AnnotatedElementUtils
                                .findMergedAnnotation(handlerType, ApiVersion.class));
    }
}