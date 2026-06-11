package com.roadmap.securevault.tenant.service;

import com.roadmap.securevault.common.config.properties.RedisProperties;
import com.roadmap.securevault.common.service.BaseRedisEntityTokenService;
import com.roadmap.securevault.common.service.RedisTokenService;
import org.springframework.stereotype.Service;

@Service
public class BootstrapTokenService extends BaseRedisEntityTokenService {

    private final RedisProperties redisProperties;

    public BootstrapTokenService(RedisTokenService redisTokenService,
                                 RedisProperties redisProperties) {
        super(redisTokenService);
        this.redisProperties = redisProperties;
    }

    @Override
    protected String prefix() { return redisProperties.bootstrapTokenPrefix(); }

    @Override
    protected long ttlHours() { return redisProperties.bootstrapTokenTtlHours(); }
}