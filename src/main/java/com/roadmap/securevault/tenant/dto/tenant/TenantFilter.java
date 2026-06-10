package com.roadmap.securevault.tenant.dto.tenant;

import com.roadmap.securevault.tenant.entity.enums.TenantStatus;

public record TenantFilter(
        TenantStatus status,
        String companyName,
        String companyCode
) {
}

