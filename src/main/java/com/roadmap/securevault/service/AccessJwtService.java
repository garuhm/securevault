package com.roadmap.securevault.service;

import com.roadmap.securevault.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AccessJwtService {
    private final JwtProperties jwtProperties;

    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.accessTokenExpiration()))
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
            throw new JwtException("Invalid JWT token", e);
        }
    }

    public String extractUsername(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (ExpiredJwtException e) {
            throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "Token has expired", e);
        } catch (JwtException e) {
            throw new JwtException("Invalid JWT token", e);
        }
    }

    public Date extractExpiration(String token) {
        try {
            return extractAllClaims(token).getExpiration();
        } catch (ExpiredJwtException e) {
            throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "Token has expired", e);
        } catch (JwtException e) {
            throw new JwtException("Invalid JWT token", e);
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            return extractUsername(token).equals(userDetails.getUsername());
        } catch (JwtException e) {
            return false;
        }
    }
}
