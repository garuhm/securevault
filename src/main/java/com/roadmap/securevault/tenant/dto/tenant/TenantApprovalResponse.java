package com.roadmap.securevault.tenant.dto.tenant;

public record TenantApprovalResponse(
        String companyName,
        String companyCode,
        String bootstrapToken
) {}
