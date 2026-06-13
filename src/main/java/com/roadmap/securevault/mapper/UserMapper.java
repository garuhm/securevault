package com.roadmap.securevault.mapper;

import com.roadmap.securevault.dto.MeResponse;
import com.roadmap.securevault.entity.User;

public class UserMapper {

    public static MeResponse toMeResponse(User user) {
        return new MeResponse(user.getUsername(), user.getEmail());
    }
}
