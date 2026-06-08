package com.roadmap.securevault.service;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.dto.PendingRegistrationAutofillInfo;
import com.roadmap.securevault.dto.PendingRegistrationData;
import com.roadmap.securevault.dto.PendingRegistrationRequest;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.exception.OAuth2PendingRegistrationException;
import com.roadmap.securevault.service.helper.CookieService;
import com.roadmap.securevault.service.helper.PendingRegJwtService;
import com.roadmap.securevault.service.helper.PendingRegistrationRedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PendingRegistrationService {
    private final CookieProperties cookieProperties;

    private final PendingRegistrationRedisService pendingRegistrationRedisService;
    private final PendingRegJwtService pendingRegJwtService;
    private final CookieService cookieService;
    private final OAuth2Service oAuth2Service;
    private final AuthService authService;

    public PendingRegistrationAutofillInfo getPendingRegistration(HttpServletRequest request) {
        String pendingRegJwtToken =
                Optional.ofNullable(
                        cookieService.extractTokenFromCookie(request, cookieProperties.oauth2PendingRegRequestCookieName()))
                        .orElseThrow(() -> new OAuth2PendingRegistrationException("No pending registration cookie found"));

        try {
            UUID pendingRegId = UUID.fromString(pendingRegJwtService.extractJti(pendingRegJwtToken));

            PendingRegistrationData pendingRegistrationData = pendingRegistrationRedisService.find(pendingRegId.toString())
                    .orElseThrow(() -> new OAuth2PendingRegistrationException("Pending registration with id " + pendingRegId + " not found"));

            return new PendingRegistrationAutofillInfo(pendingRegistrationData.email());
        }

        catch (IllegalArgumentException e) {
            throw new OAuth2PendingRegistrationException("Invalid JWT token format");
        }
    }

    @Transactional
    public void completePendingRegistration(PendingRegistrationRequest pendingRegRequest,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        String pendingRegJwtToken =
                Optional.ofNullable(
                                cookieService.extractTokenFromCookie(request, cookieProperties.oauth2PendingRegRequestCookieName()))
                        .orElseThrow(() -> new OAuth2PendingRegistrationException("No pending registration cookie found"));

        PendingRegistrationData pendingRegistrationData;
        UUID pendingRegId;
        try {
            pendingRegId = UUID.fromString(pendingRegJwtService.extractJti(pendingRegJwtToken));

            pendingRegistrationData = pendingRegistrationRedisService.find(pendingRegId.toString())
                    .orElseThrow(() -> new OAuth2PendingRegistrationException("Pending registration with id " + pendingRegId + " not found"));
        }
        catch (IllegalArgumentException e) {
            throw new OAuth2PendingRegistrationException("Invalid JWT token format");
        }

        User user = authService.register(pendingRegRequest, pendingRegistrationData, response);
        oAuth2Service.createNewLink(
                user,
                pendingRegistrationData.provider(),
                pendingRegistrationData.providerUserId(),
                pendingRegistrationData.email()
        );

        pendingRegistrationRedisService.delete(pendingRegId.toString());
        cookieService.clearCookie(response,
                cookieProperties.oauth2PendingRegRequestCookieName(),
                cookieProperties.oauth2PendingRegRequestCookiePath());
    }
}
