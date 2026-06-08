package com.roadmap.securevault.service;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.JwtProperties;
import com.roadmap.securevault.service.helper.CookieService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
@ExtendWith(MockitoExtension.class)
@DisplayName("CookieService Unit Tests")
class CookieServiceTest {
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
            "/auth/refresh",
            "securevault-oauth2-request",
            180,
            "securevault-oauth2-pending-reg-request",
            "/oauth2/pending-registration",
            "securevault-oauth2-linking-request"

    );

    @InjectMocks
    private CookieService cookieService;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @DisplayName("Add token cookies; successful")
    @Test
    void addTokenCookies_successful() {
        cookieService.addTokenCookies(response, "accessToken", "refreshToken");
        Cookie refreshCookie = Arrays.stream(response.getCookies())
                .filter(
                        cookie -> cookie.getName()
                                .equals(cookieProperties.refreshTokenCookieName()))
                .findFirst()
                .get();

        Cookie accessCookie = Arrays.stream(response.getCookies())
                .filter(
                        cookie -> cookie.getName()
                                .equals(cookieProperties.accessTokenCookieName()))
                .findFirst()
                .get();
        assertNotNull(accessCookie.getValue());
        assertEquals("/", accessCookie.getPath());
        assertEquals(jwtProperties.accessTokenExpiration() / 1000, accessCookie.getMaxAge());
        assertTrue(accessCookie.isHttpOnly());
        assertFalse(accessCookie.getSecure());

        assertNotNull(refreshCookie.getValue());
        assertEquals(cookieProperties.refreshTokenCookiePath(), refreshCookie.getPath());
        assertEquals(jwtProperties.refreshTokenExpiration() / 1000, refreshCookie.getMaxAge());
        assertTrue(refreshCookie.isHttpOnly());
        assertFalse(refreshCookie.getSecure());
    }

    @DisplayName("Clear token cookies; successful")
    @Test
    void clearTokenCookies_successful() {
        cookieService.clearTokenCookies(response);
        Cookie refreshCookie = Arrays.stream(response.getCookies())
                .filter(
                        cookie -> cookie.getName()
                                .equals(cookieProperties.refreshTokenCookieName()))
                .findFirst()
                .get();

        Cookie accessCookie = Arrays.stream(response.getCookies())
                .filter(
                        cookie -> cookie.getName()
                                .equals(cookieProperties.accessTokenCookieName()))
                .findFirst()
                .get();
        assertEquals("", accessCookie.getValue());
        assertEquals("/", accessCookie.getPath());
        assertEquals(0, accessCookie.getMaxAge());

        assertEquals("", refreshCookie.getValue());
        assertEquals(cookieProperties.refreshTokenCookiePath(), refreshCookie.getPath());
        assertEquals(0, refreshCookie.getMaxAge());
    }

    @DisplayName("Extract token from cookies; successful")
    @Test
    void extractTokenFromCookies_successful() {
        request.setCookies(
                new Cookie(cookieProperties.refreshTokenCookieName(), "refreshToken"),
                new Cookie(cookieProperties.accessTokenCookieName(), "accessToken"));

        assertEquals("refreshToken", cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName()));
        assertEquals("accessToken", cookieService.extractTokenFromCookie(request, cookieProperties.accessTokenCookieName()));
    }

    @DisplayName("Extract token from cookies with no cookies; successful returns null")
    @Test
    void extractTokenFromCookies_cookieNotFound() {
        assertNull(cookieService.extractTokenFromCookie(request, cookieProperties.accessTokenCookieName()));
        assertNull(cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName()));
    }
}
