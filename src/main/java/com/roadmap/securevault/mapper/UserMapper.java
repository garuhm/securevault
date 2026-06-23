package com.roadmap.securevault.mapper;

import com.roadmap.securevault.dto.me.MeResponse;
import com.roadmap.securevault.dto.user.UserResponse;
import com.roadmap.securevault.entity.User;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles()
        );
    }

    public static MeResponse toMeResponse(User user) {
        return new MeResponse(user.getUsername(), user.getEmail());
    }

    public static void updateUser(User user, String username, String email) {
        if(validString(username)) user.setUsername(username);
        if(validString(email)) user.setEmail(email);;
    }

    private static boolean validString(String value) {
        return value != null && !value.isBlank();
    }
}
