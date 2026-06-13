package com.roadmap.securevault.dto;

public record KeycloakTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,        // seconds
        long refreshExpiresIn  // seconds
) {
}