package com.roadmap.securevault.platform.entity.enums;

import org.springframework.security.core.GrantedAuthority;

public enum PlatformRole implements GrantedAuthority {
    PLATFORM_OWNER,
    PLATFORM_ADMIN,
    PLATFORM_SUPPORT;

    @Override
    public String getAuthority() {
        return name();
    }
}