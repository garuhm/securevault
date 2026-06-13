package com.roadmap.securevault.service.helper;

import com.roadmap.securevault.dto.KeycloakTokenResponse;
import com.roadmap.securevault.dto.RegisterRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class KeycloakAuthClient {

    public KeycloakTokenResponse passwordGrantLogin(String username, String password) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public KeycloakTokenResponse refreshGrant(String refreshToken) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void logout(String refreshToken) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public UUID createUser(RegisterRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void logoutAllSessions(UUID userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
