package com.roadmap.securevault.dto.web;

public record UserFilter(
        String username,
        String email,
        Boolean enabled
) {
}
