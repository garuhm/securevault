package com.roadmap.securevault.platform.mapper;

import com.roadmap.securevault.common.dto.MeResponse;
import com.roadmap.securevault.platform.dto.PlatformRegisterRequest;
import com.roadmap.securevault.platform.entity.PlatformUser;

public class PlatformUserMapper {

    public static PlatformUser toEntity(String username, String email, String password) {
        return PlatformUser.builder()
                .username(username)
                .email(email)
                .password(password)
                .build();
    }

    public static PlatformUser toEntity(PlatformRegisterRequest request) {
        return toEntity(
                request.username(),
                request.email(),
                request.password());
    }

    public static MeResponse toMeResponse(PlatformUser user) {
        return new MeResponse(user.getUsername(), user.getEmail());
    }
}