package com.roadmap.securevault.common.service;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.config.properties.JwtProperties;
import com.roadmap.securevault.common.dto.JwtRotationResult;
import com.roadmap.securevault.common.entity.BaseRefreshToken;
import com.roadmap.securevault.common.entity.BaseUser;
import com.roadmap.securevault.common.exception.InvalidRefreshTokenException;
import com.roadmap.securevault.common.repo.BaseRefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;

import java.util.Date;
import java.util.UUID;

public abstract class BaseRefreshJwtService<U extends BaseUser & UserDetails, T extends BaseRefreshToken<U>> {

    protected final CookieService cookieService;
    protected final AccessJwtService accessJwtService;
    protected final JwtProperties jwtProperties;
    protected final CookieProperties cookieProperties;
    protected final BaseRefreshTokenRepository<T, U> refreshTokenRepository;

    protected BaseRefreshJwtService(CookieService cookieService,
                                    AccessJwtService accessJwtService,
                                    JwtProperties jwtProperties,
                                    CookieProperties cookieProperties,
                                    BaseRefreshTokenRepository<T, U> refreshTokenRepository) {
        this.cookieService = cookieService;
        this.accessJwtService = accessJwtService;
        this.jwtProperties = jwtProperties;
        this.cookieProperties = cookieProperties;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    protected abstract T createTokenEntity(U user, Date expiryDate);

    @Transactional
    public T generateRefreshToken(U user) {
        T refreshToken = createTokenEntity(user, new Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration()));
        refreshTokenRepository.saveAndFlush(refreshToken);
        return refreshToken;
    }

    @Transactional
    public JwtRotationResult<U> validateAndRotate(HttpServletRequest request) {
        String token = cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName());
        if (token == null) {
            throw new InvalidCookieException("Refresh token cookie not found in request.");
        }

        UUID parsedToken;
        try {
            parsedToken = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new InvalidRefreshTokenException("Invalid refresh token format in cookie " + cookieProperties.refreshTokenCookieName() + ".");
        }

        T existingToken = refreshTokenRepository.findByIdAndRevokedFalse(parsedToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found."));

        if (existingToken.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token is revoked.");
        }
        if (existingToken.getExpiryDate().before(new Date())) {
            throw new InvalidRefreshTokenException("Refresh token is expired.");
        }

        existingToken.setRevoked(true);
        refreshTokenRepository.saveAndFlush(existingToken);

        U user = existingToken.getUser();
        String newAccessToken = accessJwtService.generateAccessToken(user);

        return new JwtRotationResult<>(newAccessToken, generateRefreshToken(user).getId(), user);
    }

    @Transactional
    public void revokeToken(UUID token) {
        refreshTokenRepository.findByIdAndRevokedFalse(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.saveAndFlush(refreshToken);
        });
    }

    @Transactional
    public void revokeAllTokensForUser(U user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Transactional
    public void cleanUpRevokedAndExpired() {
        refreshTokenRepository.deleteByExpiryDateBefore(new Date());
        refreshTokenRepository.deleteByRevokedTrue();
    }
}
