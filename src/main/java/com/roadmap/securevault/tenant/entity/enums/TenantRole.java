package com.roadmap.securevault.tenant.entity.enums;

import org.springframework.security.core.GrantedAuthority;

public enum TenantRole implements GrantedAuthority {
    TENANT_OWNER,
    TENANT_ADMIN,
    TENANT_MEMBER;

    @Override
    public String getAuthority() {
        return name();
    }
}
