package com.roadmap.securevault.service;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.dto.KeycloakTokenResponse;
import com.roadmap.securevault.dto.LoginRequest;
import com.roadmap.securevault.dto.RegisterRequest;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.exception.InvalidRefreshTokenException;
import com.roadmap.securevault.service.helper.CookieService;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final KeycloakAuthClient keycloakAuthClient;
    private final CookieService cookieService;
    private final CookieProperties cookieProperties;

    public void register(RegisterRequest credentials, HttpServletResponse response) {
        keycloakAuthClient.createUser(credentials);

        KeycloakTokenResponse tokens = keycloakAuthClient.passwordGrantLogin(
                credentials.username(), credentials.password());

        cookieService.addTokenCookies(response,
                tokens.accessToken(),
                tokens.expiresIn(),
                tokens.refreshToken(),
                tokens.refreshExpiresIn());
    }

    public void login(LoginRequest credentials, HttpServletResponse response) {
        KeycloakTokenResponse tokens = keycloakAuthClient.passwordGrantLogin(
                credentials.username(), credentials.password());

        cookieService.addTokenCookies(response,
                tokens.accessToken(),
                tokens.expiresIn(),
                tokens.refreshToken(),
                tokens.refreshExpiresIn());
    }

    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.extractTokenFromCookie(
                request, cookieProperties.refreshTokenCookieName());

        if (refreshToken == null) {
            throw new InvalidRefreshTokenException("Refresh token cookie not found in request.");
        }

        KeycloakTokenResponse tokens = keycloakAuthClient.refreshGrant(refreshToken);

        cookieService.addTokenCookies(response,
                tokens.accessToken(),
                tokens.expiresIn(),
                tokens.refreshToken(),
                tokens.refreshExpiresIn());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.extractTokenFromCookie(
                request, cookieProperties.refreshTokenCookieName());

        if (refreshToken != null) {
            keycloakAuthClient.logout(refreshToken);
        }
        cookieService.clearTokenCookies(response);
    }

    public void logoutAllSessions(HttpServletResponse response) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        keycloakAuthClient.logoutAllSessions(user.getId());
        cookieService.clearTokenCookies(response);
    }
}