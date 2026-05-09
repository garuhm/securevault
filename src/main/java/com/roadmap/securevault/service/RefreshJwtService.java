package com.roadmap.securevault.service;

import com.roadmap.securevault.config.properties.JwtProperties;
import com.roadmap.securevault.entity.RefreshToken;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.exception.InvalidRefreshTokenException;
import com.roadmap.securevault.repo.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshJwtService {
    private final AccessJwtService accessJwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken findByUser(User user) {
        return refreshTokenRepository
                .findFirstByUserAndRevokedFalse(user)
                .orElseGet(() -> generateRefreshToken(user));
    }

    @Transactional
    public RefreshToken generateRefreshToken(User user) {
        if(!refreshTokenRepository.findByUserAndRevokedFalse(user).isEmpty()) {
            revokeAllTokensForUser(user);
        }

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .expiryDate(new Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration()))
                .build();

        refreshTokenRepository.saveAndFlush(refreshToken);

        return refreshToken;
    }

    @Transactional
    public JwtRotationResult validateAndRotate(UUID id) {
        RefreshToken existingToken = refreshTokenRepository.findById(id)
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

    public void revokeToken(UUID token) {
        refreshTokenRepository.findByIdAndRevokedFalse(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.saveAndFlush(refreshToken);
        });
    }

    @Transactional
    public void revokeAllTokensForUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }


    public record JwtRotationResult(String accessToken, UUID refreshToken, User user) {
    }
}
