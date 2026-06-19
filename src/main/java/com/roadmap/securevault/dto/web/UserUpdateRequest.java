package com.roadmap.securevault.dto.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank(groups = Full.class)
        String username,

        @NotBlank(groups = Full.class)
        @Email
        String email,

        Boolean enabled,

        Boolean emailVerified
) {
    public interface Full {}
    public interface Partial {}
}