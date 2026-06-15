package com.roadmap.securevault.config;

import com.roadmap.securevault.config.properties.KeycloakProperties;
import com.roadmap.securevault.security.UserPrincipalOpaqueTokenAuthenticationConverter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration

@RequiredArgsConstructor
public class SecurityConfig {
    private final KeycloakProperties keycloakProperties;
    private final UserPrincipalOpaqueTokenAuthenticationConverter userPrincipalOpaqueTokenConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers("/auth/register").permitAll()
                                .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .opaqueToken(
                                opaque -> opaque
                                        .introspectionUri(keycloakProperties.introspectionUri())
                                        .introspectionClientCredentials(
                                                keycloakProperties.serviceClientId(),
                                                keycloakProperties.serviceClientSecret())
                                        .authenticationConverter(userPrincipalOpaqueTokenConverter)))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Forbidden\"}");
                        })
                );
        return http.build();
    }
}

