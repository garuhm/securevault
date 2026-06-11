package com.roadmap.securevault.platform.dto;

import com.roadmap.securevault.common.dto.UserResponse;
import com.roadmap.securevault.platform.entity.enums.PlatformRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlatformUserResponse(
        UUID id,
        String username,
        String email,
        PlatformRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements UserResponse {
}
