package com.roadmap.securevault.service.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.roadmap.securevault.config.properties.RedisProperties;
import com.roadmap.securevault.dto.PendingRegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PendingRegistrationStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisProperties redisProperties;

    public void save(String tokenId, PendingRegistrationRequest data) throws JsonProcessingException {
        String key = redisProperties.prefix() + tokenId;
        String value = objectMapper.writeValueAsString(data);

        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(redisProperties.ttl()));
    }

    public Optional<PendingRegistrationRequest> find(String tokenId) throws JsonProcessingException {
        String value = redisTemplate.opsForValue().get(redisProperties.prefix() + tokenId);
        if (value == null) return Optional.empty();
        
        return Optional.of(objectMapper.readValue(value, PendingRegistrationRequest.class));
    }

    public void delete(String tokenId) {
        redisTemplate.delete(redisProperties.prefix() + tokenId);
    }
}