package com.roadmap.securevault.tenant.dto.tenant;

import com.roadmap.securevault.tenant.entity.enums.TenantStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String companyName,
        String companyCode,
        TenantStatus status,
        String ownerEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}