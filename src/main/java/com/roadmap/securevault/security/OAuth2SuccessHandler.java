package com.roadmap.securevault.security;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.OAuth2Properties;
import com.roadmap.securevault.config.properties.RedisProperties;
import com.roadmap.securevault.dto.PendingRegistrationData;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.service.helper.PendingRegistrationRedisService;
import com.roadmap.securevault.service.helper.AccessJwtService;
import com.roadmap.securevault.service.helper.CookieService;
import com.roadmap.securevault.service.helper.PendingRegJwtService;
import com.roadmap.securevault.service.helper.RefreshJwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final CookieProperties cookieProperties;
    private final OAuth2Properties oauth2Properties;
    private final RedisProperties redisProperties;
    private final CookieService cookieService;
    private final AccessJwtService accessJwtService;
    private final RefreshJwtService refreshJwtService;
    private final PendingRegJwtService pendingRegJwtService;
    private final PendingRegistrationRedisService pendingRegistrationRedisService;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String linkingUsername = cookieService.getCookieValue(
                request, cookieProperties.oauth2LinkingRequestCookieName()
        ).orElse(null);

        cookieService.clearCookie(response, cookieProperties.oauth2LinkingRequestCookieName(), "/");

        if (linkingUsername != null) {
            // linking flow — user already has tokens, just return 200
            response.sendRedirect(oauth2Properties.successRedirectUrl());
        } else {

            // if pending registration, create redis entry
            OAuth2User principal = (OAuth2User) authentication.getPrincipal();
            if(principal instanceof PendingOAuth2User pendingUser) {

                UUID pendingRegId = UUID.randomUUID();
                PendingRegistrationData data = new PendingRegistrationData(
                        pendingUser.getEmail(),
                        pendingUser.getProvider(),
                        pendingUser.getProviderUserId()
                );
                // create redis entry
                pendingRegistrationRedisService.save(pendingRegId.toString(), data);

                String pendingRegJwt = pendingRegJwtService.generatePendingRegistrationToken(pendingRegId);
                cookieService.addCookie(
                        response,
                        cookieProperties.oauth2PendingRegRequestCookieName(),
                        cookieProperties.oauth2PendingRegRequestCookiePath(),
                        pendingRegJwt,
                        redisProperties.ttl() * 60
                );

                response.sendRedirect(oauth2Properties.pendingRegRedirectUrl());
            } else {
                User user = ((CustomOAuth2User) principal).getUser();
                cookieService.addTokenCookies(
                        response,
                        accessJwtService.generateAccessToken(user),
                        refreshJwtService.generateRefreshToken(user).getId().toString()
                );
                response.sendRedirect(oauth2Properties.successRedirectUrl());
            }
        }
    }
}
