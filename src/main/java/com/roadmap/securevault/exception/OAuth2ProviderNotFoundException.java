package com.roadmap.securevault.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public class OAuth2ProviderNotFoundException extends OAuth2AuthenticationException {
    public OAuth2ProviderNotFoundException(String message) {
        super(message);
    }
}
