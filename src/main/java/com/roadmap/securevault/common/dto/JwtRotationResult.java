package com.roadmap.securevault.common.dto;

import com.roadmap.securevault.common.entity.BaseUser;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public record JwtRotationResult<U extends BaseUser & UserDetails>(
        String accessToken,
        UUID refreshToken,
        U user
) {}
