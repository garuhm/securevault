package com.roadmap.securevault.tenant.config;

import com.roadmap.securevault.common.config.BaseSecurityConfig;
import com.roadmap.securevault.tenant.filter.TenantJwtFilter;
import com.roadmap.securevault.tenant.filter.TenantResolutionFilter;
import com.roadmap.securevault.tenant.service.TenantUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Order(2)
public class TenantSecurityConfig extends BaseSecurityConfig {

    private final TenantUserService tenantUserService;
    private final TenantJwtFilter tenantJwtFilter;
    private final TenantResolutionFilter tenantResolutionFilter;

    public TenantSecurityConfig(PasswordEncoder passwordEncoder,
                                TenantUserService tenantUserService,
                                TenantJwtFilter tenantJwtFilter,
                                TenantResolutionFilter tenantResolutionFilter) {
        super(passwordEncoder);
        this.tenantUserService = tenantUserService;
        this.tenantJwtFilter = tenantJwtFilter;
        this.tenantResolutionFilter = tenantResolutionFilter;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain tenantSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/t/**");

        http.addFilterBefore(tenantResolutionFilter, UsernamePasswordAuthenticationFilter.class);

        applyCommonSecurity(http, tenantJwtFilter)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/t/*/auth/login").permitAll()
                        .requestMatchers("/t/*/auth/setup").permitAll()
                        .requestMatchers("/t/*/auth/register").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(tenantAuthProvider());

        return http.build();
    }

    @Bean
    public AuthenticationProvider tenantAuthProvider() {
        return buildAuthProvider(tenantUserService);
    }
}