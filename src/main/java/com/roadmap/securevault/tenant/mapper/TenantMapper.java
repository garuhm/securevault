package com.roadmap.securevault.tenant.mapper;

import com.roadmap.securevault.tenant.dto.tenant.TenantResponse;
import com.roadmap.securevault.tenant.entity.Tenant;

public class TenantMapper {
    public static TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getCompanyName(),
                tenant.getCompanyCode(),
                tenant.getStatus(),
                tenant.getOwnerEmail(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
