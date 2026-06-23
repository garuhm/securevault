package com.roadmap.securevault.dto.user;

import com.roadmap.securevault.entity.enums.RoleName;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        Set<RoleName> roles
) {
}
