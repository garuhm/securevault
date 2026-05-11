package com.roadmap.securevault.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.cookie")
public record CookieProperties(
        String accessTokenCookieName,
        String refreshTokenCookieName,
        String refreshTokenCookiePath,
        String oauth2RequestCookieName,
        int oauth2RequestCookieMaxAge
) {
}
