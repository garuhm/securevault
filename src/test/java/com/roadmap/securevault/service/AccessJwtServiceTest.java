package com.roadmap.securevault.service;

import com.roadmap.securevault.config.properties.JwtProperties;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.service.helper.AccessJwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessJwtService Unit Tests")
class AccessJwtServiceTest {
    private User user;

    private static final String USERNAME = "username";
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Spy
    private final JwtProperties jwtProperties = new JwtProperties(
            "vTpTSehgudwctoSkCRB3HCdrW8NJBRA1s02pf7TXh9E",
            900000L,
            604800000L
    );

    @InjectMocks
    private AccessJwtService accessJwtService;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username(USERNAME)
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Extract all claims; successful")
    void extractAllClaims() {
        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + jwtProperties.accessTokenExpiration());
        String token = Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(jwtProperties.secretKey())
                .compact();

        Map<String, Object> claims = accessJwtService.extractAllClaims(token);

        assertNotNull(claims);
        assertEquals(USERNAME, claims.get("sub"));
        assertEquals(issuedAt.toInstant().getEpochSecond(), claims.get("iat"));
        assertEquals(expiration.toInstant().getEpochSecond(), claims.get("exp"));
    }

    @Test
    @DisplayName("Extract all claims on expired JWT; exception thrown")
    void extractAllClaimsOnExpiredJwt() {
        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration());
        String token = Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(jwtProperties.secretKey())
                .compact();

        assertThrows(ExpiredJwtException.class, () -> accessJwtService.extractAllClaims(token));
    }

    @Test
    @DisplayName("Extract all claims on invalid JWT; exception thrown")
    void extractAllClaimsOnInvalidJwt() {
        String invalidToken = "invalid.jwt.token";

        assertThrows(UnsupportedJwtException.class, () -> accessJwtService.extractAllClaims(invalidToken));
        assertFalse(accessJwtService.isTokenValid(invalidToken, user));
    }
}
