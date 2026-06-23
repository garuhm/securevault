package com.roadmap.securevault.dto.user;

import com.roadmap.securevault.entity.enums.RoleName;

import java.util.Set;

public record UserFilter(
        String username,
        String email,
        Set<RoleName> includeRoles,
        Set<RoleName> excludeRoles
) {
}
