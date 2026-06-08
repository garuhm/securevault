package com.roadmap.securevault.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "data.redis")
public record RedisProperties(
        int ttl,
        String prefix
) {
}
