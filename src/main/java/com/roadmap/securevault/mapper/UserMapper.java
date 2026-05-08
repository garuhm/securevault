package com.roadmap.securevault.mapper;

import com.roadmap.securevault.dto.CreateUserRequest;
import com.roadmap.securevault.entity.User;

public class UserMapper {

    public static User toEntity(CreateUserRequest request) {
        return User.builder()
                .username(request.username())
                .email(request.email())
                .password(request.password())
                .build();
    }

    public static CreateUserRequest toDto(User user) {
        return new CreateUserRequest(
                user.getUsername(),
                user.getEmail(),
                user.getPassword()
        );
    }
}
