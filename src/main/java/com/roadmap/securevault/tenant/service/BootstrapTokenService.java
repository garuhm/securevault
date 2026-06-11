package com.roadmap.securevault.tenant.service;

import com.roadmap.securevault.common.config.properties.RedisProperties;
import com.roadmap.securevault.common.service.RedisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BootstrapTokenService {

    private final RedisTokenService redisTokenService;
    private final RedisProperties redisTokenProperties;

    public String generateBootstrapToken(UUID tenantId) {
        return redisTokenService.generateToken(
                redisTokenProperties.bootstrapTokenPrefix(),
                tenantId.toString(),
                redisTokenProperties.bootstrapTokenTtlHours()
        );
    }

    public Optional<UUID> consumeBootstrapToken(String token) {
        return redisTokenService.consumeToken(redisTokenProperties.bootstrapTokenPrefix(), token)
                .map(UUID::fromString);
    }

    public Optional<UUID> peekBootstrapToken(String token) {
        return redisTokenService.peekToken(redisTokenProperties.bootstrapTokenPrefix(), token)
                .map(UUID::fromString);
    }
}