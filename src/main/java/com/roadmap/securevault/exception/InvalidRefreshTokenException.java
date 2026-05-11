package com.roadmap.securevault.exception;

import io.jsonwebtoken.JwtException;

public class InvalidRefreshTokenException extends JwtException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
