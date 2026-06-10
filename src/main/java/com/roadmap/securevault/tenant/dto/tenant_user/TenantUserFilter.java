package com.roadmap.securevault.tenant.dto.tenant_user;

import com.roadmap.securevault.tenant.entity.enums.TenantRole;

public record TenantUserFilter(
        TenantRole role,
        String username,
        String email
) {}