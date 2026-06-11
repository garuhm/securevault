package com.roadmap.securevault.common.service;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class BaseRedisEntityTokenService {

    protected final RedisTokenService redisTokenService;

    protected abstract String prefix();
    protected abstract long ttlHours();

    public String generateToken(UUID entityId) {
        return redisTokenService.generateToken(prefix(), entityId.toString(), ttlHours());
    }

    public Optional<UUID> consumeToken(String token) {
        return redisTokenService.consumeToken(prefix(), token)
                .map(UUID::fromString);
    }

    public Optional<UUID> peekToken(String token) {
        return redisTokenService.peekToken(prefix(), token)
                .map(UUID::fromString);
    }

    public boolean tokenExists(String token) {
        return redisTokenService.tokenExists(prefix(), token);
    }
}