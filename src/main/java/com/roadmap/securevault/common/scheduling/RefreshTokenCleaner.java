package com.roadmap.securevault.common.scheduling;

import com.roadmap.securevault.platform.service.PlatformRefreshJwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleaner {

    private final PlatformRefreshJwtService platformRefreshJwtService;
//    private final TenantRefreshJwtService tenantRefreshJwtService;

    @Scheduled(fixedRate = 900000) // 15 minutes
    public void cleanUpRevokedAndExpired() {
        platformRefreshJwtService.cleanUpRevokedAndExpired();
//        tenantRefreshJwtService.cleanUpRevokedAndExpired();
    }
}