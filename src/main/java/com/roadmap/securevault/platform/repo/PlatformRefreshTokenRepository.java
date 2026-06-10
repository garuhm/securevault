package com.roadmap.securevault.platform.repo;

import com.roadmap.securevault.common.repo.BaseRefreshTokenRepository;
import com.roadmap.securevault.platform.entity.PlatformRefreshToken;
import com.roadmap.securevault.platform.entity.PlatformUser;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformRefreshTokenRepository
        extends BaseRefreshTokenRepository<PlatformRefreshToken, PlatformUser> {
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE PlatformRefreshToken t SET t.revoked = true WHERE t.user = :user AND t.revoked = false")
    void revokeAllByUser(@Param("user") PlatformUser user);
}