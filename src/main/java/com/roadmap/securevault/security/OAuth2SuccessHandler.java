package com.roadmap.securevault.security;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.Re;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.service.helper.AccessJwtService;
import com.roadmap.securevault.service.helper.CookieService;
import com.roadmap.securevault.service.helper.RefreshJwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final CookieProperties cookieProperties;
    private final Re oauth2Properties;
    private final CookieService cookieService;
    private final AccessJwtService accessJwtService;
    private final RefreshJwtService refreshJwtService;

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
            response.sendRedirect(oauth2Properties.redirectUrl());
        } else {
            CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
            User user = principal.getUser();

            cookieService.addTokenCookies(
                    response,
                    accessJwtService.generateAccessToken(user),
                    refreshJwtService.generateRefreshToken(user).getId().toString()
            );
            response.sendRedirect(oauth2Properties.redirectUrl());
        }
    }
}
