package com.roadmap.securevault.service.helper;

import com.roadmap.securevault.config.properties.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class CookieService {
    private final CookieProperties cookieProperties;

    public void addTokenCookies(HttpServletResponse response, String accessToken, long accessTokenExpiresInSeconds,
                                String refreshToken, long refreshTokenExpiresInSeconds) {
        Cookie access = new Cookie(cookieProperties.accessTokenCookieName(), accessToken);
        access.setPath("/");
        access.setHttpOnly(true);
        access.setSecure(false);
        access.setMaxAge((int) accessTokenExpiresInSeconds);

        Cookie refresh = new Cookie(cookieProperties.refreshTokenCookieName(), refreshToken);
        refresh.setPath(cookieProperties.refreshTokenCookiePath());
        refresh.setHttpOnly(true);
        refresh.setSecure(false);
        refresh.setMaxAge((int) refreshTokenExpiresInSeconds);

        response.addCookie(access);
        response.addCookie(refresh);
    }

    public void clearTokenCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie(cookieProperties.accessTokenCookieName(), "");
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);

        Cookie refreshCookie = new Cookie(cookieProperties.refreshTokenCookieName(), "");
        refreshCookie.setPath(cookieProperties.refreshTokenCookiePath());
        refreshCookie.setMaxAge(0);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    public String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}