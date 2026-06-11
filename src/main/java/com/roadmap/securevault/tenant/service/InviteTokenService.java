package com.roadmap.securevault.tenant.service;

import com.roadmap.securevault.common.config.properties.RedisProperties;
import com.roadmap.securevault.common.service.BaseRedisEntityTokenService;
import com.roadmap.securevault.common.service.RedisTokenService;
import org.springframework.stereotype.Service;

@Service
public class InviteTokenService extends BaseRedisEntityTokenService {

    private final RedisProperties redisProperties;

    public InviteTokenService(RedisTokenService redisTokenService,
                              RedisProperties redisProperties) {
        super(redisTokenService);
        this.redisProperties = redisProperties;
    }

    @Override
    protected String prefix() { return redisProperties.inviteTokenPrefix(); }

    @Override
    protected long ttlHours() { return redisProperties.inviteTokenTtlHours(); }
}