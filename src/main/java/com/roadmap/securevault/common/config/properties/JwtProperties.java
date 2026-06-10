package com.roadmap.securevault.common.config.properties;

import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration
) {
    public SecretKey secretKey () {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
