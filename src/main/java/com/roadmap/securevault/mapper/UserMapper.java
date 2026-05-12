package com.roadmap.securevault.mapper;

import com.roadmap.securevault.dto.MeResponse;
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

    public static MeResponse toMeResponse(User user) {
        return new MeResponse(user.getUsername(), user.getEmail());
    }
}
