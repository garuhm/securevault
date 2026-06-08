package com.roadmap.securevault.mapper;

import com.roadmap.securevault.dto.MeResponse;
import com.roadmap.securevault.dto.PendingRegistrationData;
import com.roadmap.securevault.dto.PendingRegistrationRequest;
import com.roadmap.securevault.dto.RegisterRequest;
import com.roadmap.securevault.entity.User;

public class UserMapper {

    public static User toEntity(RegisterRequest request) {
        return User.builder()
                .username(request.username())
                .email(request.email())
                .password(request.password())
                .build();
    }

    public static User toEntity(PendingRegistrationRequest request,
                                PendingRegistrationData pendingRegistrationData) {
        return User.builder()
                .username(request.username())
                .email(pendingRegistrationData.email())
                .build();
    }

    public static MeResponse toMeResponse(User user) {
        return new MeResponse(user.getUsername(), user.getEmail());
    }
}
