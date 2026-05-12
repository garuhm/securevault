package com.roadmap.securevault.exception;

public class CredentialsTakenException extends RuntimeException {
    public CredentialsTakenException(String message) {
        super(message);
    }
}
