package com.roadmap.securevault.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public class OAuth2AuthenticationUnlinkException extends OAuth2AuthenticationException {
    public OAuth2AuthenticationUnlinkException(String message) {
        super(message);
    }
}
