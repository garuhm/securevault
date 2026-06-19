package com.roadmap.securevault.dto.keycloak;

import com.roadmap.securevault.entity.enums.RoleName;

import java.util.Set;
import java.util.UUID;

public record KeycloakUserRepresentation(
        UUID id,
        String username,
        String email,
        Set<RoleName> roles
) {
}