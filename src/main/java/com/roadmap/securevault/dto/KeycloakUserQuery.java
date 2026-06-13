package com.roadmap.securevault.dto;

public record KeycloakUserQuery(
        Integer first,
        Integer max,
        String username,
        String email,
        Boolean enabled
) {
}