package com.roadmap.securevault.dto.keycloak;

import java.util.UUID;

public record KeycloakUserRepresentation(
        UUID id,
        String username,
        String email
) {
}