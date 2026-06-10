package com.roadmap.securevault.tenant.dto.tenant_user;

import com.roadmap.securevault.tenant.entity.enums.TenantRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record TenantUserResponse(
        UUID id,
        String username,
        String email,
        TenantRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
