package com.roadmap.securevault.dto;

import java.util.UUID;

public record KeycloakUserRepresentation(
        UUID id,
        String username,
        String email
) {
}