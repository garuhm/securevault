package com.roadmap.securevault.dto;

import jakarta.validation.constraints.NotEmpty;

public record PendingRegistrationRequest(
        @NotEmpty(message = "Email cannot be blank")
        String email,
        @NotEmpty(message = "Provider cannot be blank")
        String provider,
        @NotEmpty(message = "Provider User ID cannot be blank")
        String providerUserId) {
}
