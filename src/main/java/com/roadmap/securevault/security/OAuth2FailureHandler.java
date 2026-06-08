package com.roadmap.securevault.security;

import com.roadmap.securevault.exception.OAuth2AuthenticationLinkException;
import com.roadmap.securevault.exception.OAuth2CredentialsExtractionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        Throwable cause = exception.getCause() != null ? exception.getCause() : exception;

        int status = switch (cause) {
            case OAuth2AuthenticationLinkException e -> HttpServletResponse.SC_CONFLICT;
            case OAuth2CredentialsExtractionException e -> HttpServletResponse.SC_BAD_GATEWAY;
            
            default -> HttpServletResponse.SC_UNAUTHORIZED;
        };

        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(cause.getMessage()));
    }
}