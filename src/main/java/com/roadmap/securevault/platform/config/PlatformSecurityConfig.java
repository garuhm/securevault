package com.roadmap.securevault.platform.config;

import com.roadmap.securevault.common.config.security.BaseSecurityConfig;
import com.roadmap.securevault.common.util.ApiVersioningResolver;
import com.roadmap.securevault.platform.controller.PlatformAuthController;
import com.roadmap.securevault.platform.filter.PlatformJwtFilter;
import com.roadmap.securevault.platform.service.PlatformUserService;
import com.roadmap.securevault.tenant.controller.TenantController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(1)
public class PlatformSecurityConfig extends BaseSecurityConfig {

    private final PlatformUserService platformUserService;
    private final PlatformJwtFilter platformJwtFilter;

    public PlatformSecurityConfig(PasswordEncoder passwordEncoder,
                                  PlatformUserService platformUserService,
                                  PlatformJwtFilter platformJwtFilter) {
        super(passwordEncoder);
        this.platformUserService = platformUserService;
        this.platformJwtFilter = platformJwtFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain platformSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/platform/**", "/api/*/platform/**");

        String loginUrl = ApiVersioningResolver.resolve(PlatformAuthController.class, "login", "/platform/auth/login");
        String registerTenantUrl = ApiVersioningResolver.resolve(TenantController.class, "registerTenant", "/platform/tenants/register");

        applyCommonSecurity(http, platformJwtFilter)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(loginUrl).permitAll()
                        .requestMatchers(registerTenantUrl).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(platformAuthProvider());

        return http.build();
    }

    @Bean
    public AuthenticationProvider platformAuthProvider() {
        return buildAuthProvider(platformUserService);
    }
}