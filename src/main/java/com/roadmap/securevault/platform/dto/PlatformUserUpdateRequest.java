package com.roadmap.securevault.platform.dto;

import com.roadmap.securevault.common.dto.UserUpdateRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record PlatformUserUpdateRequest(
        @Pattern(regexp = "^[a-zA-Z0-9_.]{4,32}$",
                message = "Username must be between 4 and 32 characters and contain only letters, numbers, underscores, and periods")
        String username,

        @Email(message = "Invalid email format")
        String email,

        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one valid special character")
        String password
) implements UserUpdateRequest {}
