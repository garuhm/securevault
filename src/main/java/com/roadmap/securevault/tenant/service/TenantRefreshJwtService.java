package com.roadmap.securevault.tenant.service;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.config.properties.JwtProperties;
import com.roadmap.securevault.common.service.AccessJwtService;
import com.roadmap.securevault.common.service.BaseRefreshJwtService;
import com.roadmap.securevault.common.service.CookieService;
import com.roadmap.securevault.multitenancy.TenantContext;
import com.roadmap.securevault.tenant.entity.TenantRefreshToken;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.repo.TenantRefreshTokenRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class TenantRefreshJwtService extends BaseRefreshJwtService<TenantUser, TenantRefreshToken> {
    private final TenantRefreshTokenRepository tenantRefreshTokenRepository;

    @PersistenceContext private EntityManager entityManager;

    public TenantRefreshJwtService(CookieService cookieService,
                                     AccessJwtService accessJwtService,
                                     JwtProperties jwtProperties,
                                     CookieProperties cookieProperties,
                                     TenantRefreshTokenRepository refreshTokenRepository) {
        super(cookieService, accessJwtService, jwtProperties, cookieProperties, refreshTokenRepository);
        this.tenantRefreshTokenRepository = refreshTokenRepository;
    }

    @Override
    protected TenantRefreshToken createTokenEntity(TenantUser user, Date expiryDate) {
        return TenantRefreshToken.builder()
                .user(user)
                .expiryDate(expiryDate)
                .build();
    }

    @Transactional
    public void revokeAllTenantTokens(String schemaName) {
        if (!schemaExists(schemaName)) return;

        TenantContext.setTenantSchema(schemaName);
        try {
            tenantRefreshTokenRepository.revokeAllTokens();
        } finally {
            TenantContext.clear();
        }
    }

    private boolean schemaExists(String schemaName) {
        Long count = (Long) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = :schemaName")
                .setParameter("schemaName", schemaName)
                .getSingleResult();
        return count > 0;
    }
}
