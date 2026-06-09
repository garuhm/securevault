package com.roadmap.securevault.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class OAuth2AuthenticationLinkException extends OAuth2AuthenticationException {
    public OAuth2AuthenticationLinkException(String message) {
        super(new OAuth2Error("link_error", message, null), message);
    }
}