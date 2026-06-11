package com.roadmap.securevault.platform.dto;

import com.roadmap.securevault.platform.entity.enums.PlatformRole;

public record PlatformUserFilter(
        PlatformRole role,
        String username,
        String email
) {}