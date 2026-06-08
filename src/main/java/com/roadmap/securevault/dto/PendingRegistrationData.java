package com.roadmap.securevault.dto;


public record PendingRegistrationData(
        String email,
        String provider,
        String providerUserId
) {
}
