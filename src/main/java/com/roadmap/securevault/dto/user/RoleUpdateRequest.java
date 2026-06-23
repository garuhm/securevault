package com.roadmap.securevault.dto.user;

import com.roadmap.securevault.entity.enums.RoleName;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record RoleUpdateRequest(
        @NotEmpty(message = "Role name cannot be blank")
        Set<RoleName> roles
) {
}