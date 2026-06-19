package com.roadmap.securevault.dto.web;

import com.roadmap.securevault.entity.enums.RoleName;
import jakarta.validation.constraints.NotBlank;

public record RoleUpdateRequest(
        @NotBlank(message = "Role name cannot be blank")
        RoleName role
) {
}