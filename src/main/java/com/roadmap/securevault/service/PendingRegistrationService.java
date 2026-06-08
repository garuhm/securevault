package com.roadmap.securevault.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
public class PendingRegistrationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisProperties redisProperties;

    public void save(String tokenId, PendingRegistrationData data) throws JsonProcessingException {
        String key = redisProperties.prefix() + tokenId;
        String value = objectMapper.writeValueAsString(data);

        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(redisProperties.ttl()));
    }

    public Optional<PendingRegistrationData> find(String tokenId) throws JsonProcessingException {
        String value = redisTemplate.opsForValue().get(redisProperties.prefix() + tokenId);
        if (value == null) return Optional.empty();
        
        return Optional.of(objectMapper.readValue(value, PendingRegistrationData.class));
    }

    public void delete(String tokenId) {
        redisTemplate.delete(redisProperties.prefix() + tokenId);
    }
}