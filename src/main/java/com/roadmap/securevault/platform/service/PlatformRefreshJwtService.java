package com.roadmap.securevault.platform.service;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.config.properties.JwtProperties;
import com.roadmap.securevault.common.service.AccessJwtService;
import com.roadmap.securevault.common.service.BaseRefreshJwtService;
import com.roadmap.securevault.common.service.CookieService;
import com.roadmap.securevault.platform.entity.PlatformRefreshToken;
import com.roadmap.securevault.platform.entity.PlatformUser;
import com.roadmap.securevault.platform.repo.PlatformRefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class PlatformRefreshJwtService extends BaseRefreshJwtService<PlatformUser, PlatformRefreshToken> {

    public PlatformRefreshJwtService(CookieService cookieService,
                                     AccessJwtService accessJwtService,
                                     JwtProperties jwtProperties,
                                     CookieProperties cookieProperties,
                                     PlatformRefreshTokenRepository refreshTokenRepository) {
        super(cookieService, accessJwtService, jwtProperties, cookieProperties, refreshTokenRepository);
    }

    @Override
    protected PlatformRefreshToken createTokenEntity(PlatformUser user, Date expiryDate) {
        return PlatformRefreshToken.builder()
                .user(user)
                .expiryDate(expiryDate)
                .build();
    }
}