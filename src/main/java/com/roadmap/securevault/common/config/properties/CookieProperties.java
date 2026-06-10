package com.roadmap.securevault.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.cookie")
public record CookieProperties(
        String accessTokenCookieName,
        String refreshTokenCookieName,
        String refreshTokenCookiePath
) {
}
