package com.roadmap.securevault.tenant.mapper;

import com.roadmap.securevault.tenant.dto.tenant.TenantMeResponse;
import com.roadmap.securevault.tenant.dto.tenant_user.TenantUserResponse;
import com.roadmap.securevault.tenant.entity.TenantUser;

public class TenantUserMapper {
    public static TenantUserResponse toResponse(TenantUser user) {
        return new TenantUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static TenantMeResponse toMeResponse(TenantUser user, String companyCode) {
        return new TenantMeResponse(
                user.getUsername(),
                user.getEmail(),
                companyCode);
    }
}
