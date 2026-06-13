package com.roadmap.securevault.dto.keycloak;

public record KeycloakTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,        // seconds
        long refreshExpiresIn  // seconds
) {
}