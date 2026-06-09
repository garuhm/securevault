package com.roadmap.securevault.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class OAuth2CredentialsExtractionException extends OAuth2AuthenticationException {
    public OAuth2CredentialsExtractionException(String message) {
        super(new OAuth2Error("extraction_error", message, null), message);
    }
}
