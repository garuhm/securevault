package com.roadmap.securevault.exception;

public class OAuth2ProviderNotFoundException extends RuntimeException {
    public OAuth2ProviderNotFoundException(String message) {
        super(message);
    }
}
