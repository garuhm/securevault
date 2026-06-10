package com.roadmap.securevault.tenant.dto;

public record TenantMeResponse(
        String username,
        String email,
        String companyCode
) {}