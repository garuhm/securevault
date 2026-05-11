package com.roadmap.securevault.service.helper;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.JwtProperties;
import com.roadmap.securevault.entity.RefreshToken;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.exception.InvalidRefreshTokenException;
import com.roadmap.securevault.repo.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshJwtService {
    private final CookieService cookieService;
    private final AccessJwtService accessJwtService;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken generateRefreshToken(UserDetails userDetails) {
        User user = (User) userDetails;
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .expiryDate(new Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration()))
                .build();

        user.getRefreshTokens().add(refreshToken);
        refreshTokenRepository.saveAndFlush(refreshToken);

        return refreshToken;
    }

    @Transactional
    public JwtRotationResult validateAndRotate(HttpServletRequest request) {
        String token = cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName());
        if(token == null) {
            throw new InvalidCookieException("Refresh token cookie not found in request.");
        }
        UUID parsedToken;
        try {
            parsedToken = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new InvalidRefreshTokenException("Invalid refresh token format in cookie " + cookieProperties.refreshTokenCookieName() + ".");
        }

        RefreshToken existingToken = refreshTokenRepository.findById(parsedToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found."));

        if (existingToken.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token is revoked.");
        }
        if (existingToken.getExpiryDate().before(new Date())) {
            throw new InvalidRefreshTokenException("Refresh token is expired.");
        }

        existingToken.setRevoked(true);
        refreshTokenRepository.saveAndFlush(existingToken);

        User user = existingToken.getUser();
        String newAccessToken = accessJwtService.generateAccessToken(user);

        return new JwtRotationResult(newAccessToken, generateRefreshToken(user).getId(), user);
    }

    @Transactional
    public void revokeToken(UUID token) {
        refreshTokenRepository.findByIdAndRevokedFalse(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.saveAndFlush(refreshToken);
        });
    }

    @Transactional
    public void revokeAllTokensForUser(UserDetails user) {
        refreshTokenRepository.revokeAllByUser((User) user);
    }
    public record JwtRotationResult(String accessToken, UUID refreshToken, User user) {
    }
}
