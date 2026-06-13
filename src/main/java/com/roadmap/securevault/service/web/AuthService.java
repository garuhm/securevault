package com.roadmap.securevault.service.web;

import com.roadmap.securevault.config.KafkaTopics;
import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.dto.keycloak.KeycloakTokenResponse;
import com.roadmap.securevault.dto.web.LoginRequest;
import com.roadmap.securevault.dto.web.RegisterRequest;
import com.roadmap.securevault.kafka.events.UserEvent;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.exception.InvalidRefreshTokenException;
import com.roadmap.securevault.service.helper.CookieService;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final KeycloakAuthClient keycloakAuthClient;
    private final JwtDecoder jwtDecoder;
    private final CookieService cookieService;
    private final CookieProperties cookieProperties;
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public void register(RegisterRequest credentials, HttpServletResponse response) {
        keycloakAuthClient.createUser(credentials);

        KeycloakTokenResponse tokens = keycloakAuthClient.passwordGrantLogin(
                credentials.username(), credentials.password());

        Jwt jwt = jwtDecoder.decode(tokens.accessToken());

        UserEvent event = new UserEvent(
                UUID.fromString(jwt.getSubject()),
                credentials.username(),
                credentials.email(),
                UserEvent.UserEventType.CREATED
        );
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.id().toString(), event);

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