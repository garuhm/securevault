package com.roadmap.securevault.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.redis")
public record RedisProperties(
        long bootstrapTokenTtlHours,
        String bootstrapTokenPrefix,
        long inviteTokenTtlHours,
        String inviteTokenPrefix
) {}