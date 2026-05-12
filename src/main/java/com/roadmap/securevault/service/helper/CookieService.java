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

    public void addCookie(HttpServletResponse response, String cookieName, String cookieValue, String path, int maxAge) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public void clearCookie(HttpServletResponse response, String cookieName, String path) {
        addCookie(response, cookieName, "", path, 0);
    }

    public void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        addCookie(response,
                cookieProperties.accessTokenCookieName(),
                accessToken,
                "/",
                (int) (jwtProperties.accessTokenExpiration() / 1000));
        addCookie(response,
                cookieProperties.refreshTokenCookieName(),
                refreshToken,
                cookieProperties.refreshTokenCookiePath(),
                (int) (jwtProperties.refreshTokenExpiration() / 1000));
    }

    public void clearTokenCookies(HttpServletResponse response) {
        clearCookie(response,
                cookieProperties.accessTokenCookieName(),
                "/");
        clearCookie(response,
                cookieProperties.refreshTokenCookieName(),
                cookieProperties.refreshTokenCookiePath());
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
