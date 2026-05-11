package com.roadmap.securevault.service.helper;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CookieService {
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;
    
    public void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie access = new Cookie(cookieProperties.accessTokenCookieName(), accessToken);
        access.setPath("/");
        access.setHttpOnly(true);
        access.setSecure(false);
        access.setMaxAge((int) (jwtProperties.accessTokenExpiration() / 1000));
        
        Cookie refresh = new Cookie(cookieProperties.refreshTokenCookieName(), refreshToken);
        refresh.setPath(cookieProperties.refreshTokenCookiePath());
        refresh.setHttpOnly(true);
        refresh.setSecure(false);
        refresh.setMaxAge((int) (jwtProperties.refreshTokenExpiration() / 1000));
        
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

    public void clearCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();

        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        return getCookieValue(request, cookieName)
                .orElse(null);
    }
}
