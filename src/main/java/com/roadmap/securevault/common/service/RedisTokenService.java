package com.roadmap.securevault.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    public String generateToken(String keyPrefix, String value, long ttlHours) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                keyPrefix + token,
                value,
                ttlHours,
                TimeUnit.HOURS
        );
        return token;
    }

    public Optional<String> consumeToken(String keyPrefix, String token) {
        String key = keyPrefix + token;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return Optional.empty();

        redisTemplate.delete(key);
        return Optional.of(value);
    }

    public Optional<String> peekToken(String keyPrefix, String token) {
        String value = redisTemplate.opsForValue().get(keyPrefix + token);
        return Optional.ofNullable(value);
    }

    public boolean tokenExists(String keyPrefix, String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(keyPrefix + token));
    }
}