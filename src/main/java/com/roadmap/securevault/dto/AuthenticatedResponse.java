package com.roadmap.securevault.dto;

import java.util.UUID;

public record AuthenticatedResponse(
        String accessToken,
        UUID refreshToken
) {
}
