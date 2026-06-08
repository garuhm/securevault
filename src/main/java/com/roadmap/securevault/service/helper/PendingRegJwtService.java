package com.roadmap.securevault.service.helper;

import com.roadmap.securevault.config.properties.JwtProperties;
import com.roadmap.securevault.config.properties.RedisProperties;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PendingRegJwtService {
    private final RedisProperties redisProperties;
    private final JwtProperties jwtProperties;

    public String generatePendingRegistrationToken(UUID jti) {
        return Jwts.builder()
                .id(jti.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + (redisProperties.ttl() * 1000L)))
                .signWith(jwtProperties.secretKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(jwtProperties.secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "Token has expired", e);
        } catch (JwtException e) {
            throw new UnsupportedJwtException("Invalid JWT token", e);
        }
    }
    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenValid(String token, UUID jti) {
        try {
            return UUID.fromString(extractJti(token)).equals(jti)
                    && !extractExpiration(token).before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
