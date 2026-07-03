package com.roadmap.securevault.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "security.oauth2")
public record OAuth2Properties(
        List<String> providers,
        String clientId,
        String redirectUrl
) {
}