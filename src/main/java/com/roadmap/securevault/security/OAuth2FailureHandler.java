package com.roadmap.securevault.security;

import com.roadmap.securevault.exception.OAuth2AuthenticationLinkException;
import com.roadmap.securevault.exception.OAuth2CredentialsExtractionException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        Throwable cause = exception.getCause() != null ? exception.getCause() : exception;

        String message = switch (cause) {
            case OAuth2AuthenticationLinkException e -> e.getMessage();
            case OAuth2CredentialsExtractionException e -> "Could not retrieve account information from the provider.";
            default -> "Authentication failed. Please try again.";
        };

        // store in a short-lived cookie the frontend can read once
        Cookie errorCookie = new Cookie("oauth2_error", URLEncoder.encode(message, StandardCharsets.UTF_8));
        errorCookie.setPath("/");
        errorCookie.setMaxAge(30);
        errorCookie.setHttpOnly(false); // must be readable by JS
        response.addCookie(errorCookie);

        response.sendRedirect("http://localhost:3000/login.html");
    }
}