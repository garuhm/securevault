package com.roadmap.securevault.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UserResponse {
    UUID id();
    String username();
    String email();
    LocalDateTime createdAt();
    LocalDateTime updatedAt();
}
