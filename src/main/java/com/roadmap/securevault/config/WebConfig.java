package com.roadmap.securevault.config;

import com.roadmap.securevault.controller.helper.VersionedRequestMappingHandlerMapping;
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class WebConfig implements WebMvcRegistrations {
    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        RequestMappingHandlerMapping mapping = new VersionedRequestMappingHandlerMapping();
        mapping.setOrder(0);

        return mapping;
    }
}