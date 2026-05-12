package com.roadmap.securevault.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public class OAuth2AuthenticationLinkException extends OAuth2AuthenticationException {
    public OAuth2AuthenticationLinkException(String message) {
        super(message);
    }
}
