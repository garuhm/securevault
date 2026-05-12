package com.roadmap.securevault.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "security.oauth2")
public record OAuth2Properties(
        Map<String, String> providerEmailAttributes,
        Map<String, String> providerIdAttributes,
        String redirectUrl
) {
}
