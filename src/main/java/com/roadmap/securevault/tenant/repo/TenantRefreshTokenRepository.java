package com.roadmap.securevault.tenant.repo;

import com.roadmap.securevault.common.repo.BaseRefreshTokenRepository;
import com.roadmap.securevault.tenant.entity.TenantRefreshToken;
import com.roadmap.securevault.tenant.entity.TenantUser;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRefreshTokenRepository
        extends BaseRefreshTokenRepository<TenantRefreshToken, TenantUser> {

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE TenantRefreshToken t SET t.revoked = true WHERE t.user = :user AND t.revoked = false")
    void revokeAllByUser(@Param("user") TenantUser user);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE TenantRefreshToken t SET t.revoked = true WHERE t.revoked = false")
    void revokeAllTokens();
}