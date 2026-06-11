package com.roadmap.securevault.platform.mapper;

import com.roadmap.securevault.common.dto.MeResponse;
import com.roadmap.securevault.platform.dto.PlatformUserRegisterRequest;
import com.roadmap.securevault.platform.dto.PlatformUserResponse;
import com.roadmap.securevault.platform.entity.PlatformUser;

public class PlatformUserMapper {

    public static PlatformUser toEntity(PlatformUserRegisterRequest credentials) {
        return PlatformUser.builder()
                .username(credentials.username())
                .email(credentials.email())
                .password(credentials.password())
                .build();
    }

    public static PlatformUserResponse toResponse(PlatformUser user) {
        return new PlatformUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static MeResponse toMeResponse(PlatformUser user) {
        return new MeResponse(user.getUsername(), user.getEmail());
    }

}