package com.roadmap.securevault.service.helper;

import com.roadmap.securevault.config.properties.RedisProperties;
import com.roadmap.securevault.dto.PendingRegistrationData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PendingRegistrationRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisProperties redisProperties;

    public Optional<PendingRegistrationData> find(String tokenId) {
        String value = redisTemplate.opsForValue().get(redisProperties.prefix() + tokenId);
        if (value == null) return Optional.empty();
        return Optional.of(objectMapper.readValue(value, PendingRegistrationData.class));
    }

    public void save(String tokenId, PendingRegistrationData data) {
        String key = redisProperties.prefix() + tokenId;
        String value = objectMapper.writeValueAsString(data);
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(redisProperties.ttl()));
    }

    public void delete(String tokenId) {
        redisTemplate.delete(redisProperties.prefix() + tokenId);
    }
}