package com.roadmap.securevault.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record PendingRegistrationRequest(
        @NotEmpty(message = "Username cannot be blank")
        @Pattern(regexp = "^[a-zA-Z0-9_.]{4,32}$",
                message = "Username must be between 4 and 32 characters and contain only letters, numbers, underscores, and periods")
        String username
) {
}
