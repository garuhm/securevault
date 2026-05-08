package com.roadmap.securevault.exception;

import org.springframework.web.server.ServerWebInputException;

public class CredentialsTakenException extends ServerWebInputException {
    public CredentialsTakenException(String message) {
        super(message);
    }
}
