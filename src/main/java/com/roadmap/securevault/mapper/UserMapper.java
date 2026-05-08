package com.roadmap.securevault.mapper;

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

    public static RegisterRequest toDto(User user) {
        return new RegisterRequest(
                user.getUsername(),
                user.getEmail(),
                user.getPassword()
        );
    }
}
