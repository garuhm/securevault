package com.roadmap.securevault.tenant.dto.invite;

import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteCreateRequest(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Invalid email format")
        String email,

        TenantRole role
) {}
