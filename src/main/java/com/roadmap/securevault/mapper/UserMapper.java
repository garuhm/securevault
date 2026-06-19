package com.roadmap.securevault.mapper;

import com.roadmap.securevault.dto.web.MeResponse;
import com.roadmap.securevault.entity.User;

public class UserMapper {

    public static MeResponse toMeResponse(User user) {
        return new MeResponse(user.getUsername(), user.getEmail());
    }

    public static void updateUser(User user, String username, String email) {
        if(validString(username)) user.setUsername(username);
        if(validString(email)) user.setEmail(email);
    }

    private static boolean validString(String value) {
        return value != null && !value.isBlank();
    }
}
