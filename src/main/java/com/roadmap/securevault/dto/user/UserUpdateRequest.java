package com.roadmap.securevault.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserUpdateRequest(
        @NotBlank(groups = Full.class, message = "Username cannot be blank")
        @Pattern(regexp = "^[a-zA-Z0-9_.]{4,32}$",
                message = "Username must be between 4 and 32 characters and contain only letters, numbers, underscores, and periods",
                groups = {Full.class, Partial.class})
        String username,

        @NotBlank(groups = Full.class, message = "Email cannot be blank")
        @Email(message = "Invalid email format",
                groups = {Full.class, Partial.class})
        String email
) {
    public interface Full {}
    public interface Partial {}
}