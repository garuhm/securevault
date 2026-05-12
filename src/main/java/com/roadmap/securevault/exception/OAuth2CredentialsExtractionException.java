package com.roadmap.securevault.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public class OAuth2CredentialsExtractionException extends OAuth2AuthenticationException {
    public OAuth2CredentialsExtractionException(String message) {
        super(message);
    }
}
