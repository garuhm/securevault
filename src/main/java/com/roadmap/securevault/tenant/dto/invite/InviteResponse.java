package com.roadmap.securevault.tenant.dto.invite;

import com.roadmap.securevault.tenant.entity.enums.TenantRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record InviteResponse(
        UUID id,
        String code,
        TenantRole role,
        String inviteeEmail,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        LocalDateTime createdAt
) {}
