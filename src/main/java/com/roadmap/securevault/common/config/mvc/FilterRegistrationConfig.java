package com.roadmap.securevault.common.config.mvc;

import com.roadmap.securevault.platform.filter.PlatformJwtFilter;
import com.roadmap.securevault.tenant.filter.TenantJwtFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterRegistrationConfig {
//    disables global use
    @Bean
    public FilterRegistrationBean<PlatformJwtFilter> platformJwtFilterRegistration(PlatformJwtFilter filter) {
        FilterRegistrationBean<PlatformJwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<TenantJwtFilter> tenantJwtFilterRegistration(TenantJwtFilter filter) {
        FilterRegistrationBean<TenantJwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}