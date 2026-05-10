package com.roadmap.securevault.service;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.JwtProperties;
import com.roadmap.securevault.entity.RefreshToken;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.exception.InvalidRefreshTokenException;
import com.roadmap.securevault.repo.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshJwtService Unit Tests")
class RefreshJwtServiceTest {
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private CookieService cookieService;

    @Mock
    private AccessJwtService accessJwtService;

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

    @Spy
    private final CookieProperties cookieProperties = new CookieProperties(
            "securevault-access-token",
            "securevault-refresh-token",
            "/auth/refresh"
    );

    @InjectMocks
    private RefreshJwtService refreshJwtService;

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
    @DisplayName("Generate refresh token; successful")
    void generateRefreshTokenSuccessful() {
        UUID tokenValue = UUID.randomUUID();

        when(refreshTokenRepository.saveAndFlush(any(RefreshToken.class)))
                .thenAnswer(invocation -> {
                    RefreshToken refreshToken = invocation.getArgument(0);
                    refreshToken.setId(tokenValue);
                    return refreshToken;
                });

        RefreshToken refreshToken = refreshJwtService.generateRefreshToken(user);
        assertNotNull(refreshToken);
        assertEquals(tokenValue, refreshToken.getId());
        assertEquals(user, refreshToken.getUser());
        assertEquals(1, user.getRefreshTokens().size());
        assertEquals(refreshToken, user.getRefreshTokens().iterator().next());
    }

    @Nested
    @DisplayName("Validate and rotate tests")
    class ValidateAndRotate {

        @Test
        @DisplayName("Validate and rotate; successful")
        void validateAndRotate() {
            Date expiry = new Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration());

            when(cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName()))
                    .thenReturn(UUID.randomUUID().toString());
            when(refreshTokenRepository.findById(any()))
                    .thenReturn(Optional.of(
                            RefreshToken.builder()
                                    .id(UUID.randomUUID())
                                    .expiryDate(expiry)
                                    .user(user)
                                    .build()));
            when(refreshTokenRepository.saveAndFlush(any(RefreshToken.class)))
                    .thenAnswer(invocation -> {
                        RefreshToken refreshToken = invocation.getArgument(0);
                        if(refreshToken.isRevoked()) {
                            return refreshToken;
                        }
                        refreshToken.setId(UUID.randomUUID());
                        return refreshToken;
                    });
            when(accessJwtService.generateAccessToken(user))
                    .thenReturn("accessToken");

            RefreshJwtService.JwtRotationResult result = refreshJwtService.validateAndRotate(request);

            assertNotNull(result.accessToken());
            assertNotNull(result.refreshToken());
        }

        @Test
        @DisplayName("Validate and rotate with null cookie; exception thrown")
        void validateAndRotateWithNullCookie() {
            when(cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName()))
                    .thenReturn(null);

            assertThrows(InvalidCookieException.class, () -> refreshJwtService.validateAndRotate(request));
        }

        @Test
        @DisplayName("Validate and rotate with invalid refresh token format; exception thrown")
        void validateAndRotateWithInvalidRefreshTokenFormat() {
            when(cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName()))
                    .thenReturn("invalidRefreshToken");

            assertThrows(InvalidRefreshTokenException.class, () -> refreshJwtService.validateAndRotate(request));
        }

        @Test
        @DisplayName("Validate and rotate with nonexistent refresh token; exception thrown")
        void validateAndRotateWithNonexistentRefreshToken() {
            when(cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName()))
                    .thenReturn(UUID.randomUUID().toString());

            assertThrows(InvalidRefreshTokenException.class, () -> refreshJwtService.validateAndRotate(request));
        }

        @Test
        @DisplayName("Validate and rotate with expired refresh token; exception thrown")
        void validateAndRotateWithExpiredRefreshToken() {
            Date expiry = new Date(System.currentTimeMillis() - jwtProperties.refreshTokenExpiration());

            when(cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName()))
                    .thenReturn(UUID.randomUUID().toString());
            when(refreshTokenRepository.findById(any()))
                    .thenReturn(Optional.of(
                            RefreshToken.builder()
                                    .id(UUID.randomUUID())
                                    .expiryDate(expiry)
                                    .user(user)
                                    .build()));

            assertThrows(InvalidRefreshTokenException.class, () -> refreshJwtService.validateAndRotate(request));
        }

        @Test
        @DisplayName("Validate and rotate with revoked refresh token; exception thrown")
        void validateAndRotateWithRevokedRefreshToken() {
            Date expiry = new Date(System.currentTimeMillis() - jwtProperties.refreshTokenExpiration());

            when(cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName()))
                    .thenReturn(UUID.randomUUID().toString());
            when(refreshTokenRepository.findById(any()))
                    .thenReturn(Optional.of(
                            RefreshToken.builder()
                                    .id(UUID.randomUUID())
                                    .expiryDate(expiry)
                                    .revoked(true)
                                    .user(user)
                                    .build()));

            assertThrows(InvalidRefreshTokenException.class, () -> refreshJwtService.validateAndRotate(request));
        }
    }
}
