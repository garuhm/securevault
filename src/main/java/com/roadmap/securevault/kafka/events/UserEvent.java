package com.roadmap.securevault.kafka.events;

import java.util.UUID;

public record UserEvent(
        UUID id,
        String username,
        String email,
        UserEventType type
) {
    public enum UserEventType {
        CREATED, UPDATED, DELETED
    }
}