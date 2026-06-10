package com.roadmap.securevault.tenant.dto.tenant;

public record TenantMeResponse(
        String username,
        String email,
        String companyCode
) {}