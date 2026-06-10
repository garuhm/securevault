package com.roadmap.securevault.common.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!email")
public class ApiNotificationService implements NotificationService {

    @Override
    public void sendBootstrapLink(String toEmail, String token) {
        // no-op (token is returned directly in the approval response)
    }

    @Override
    public void sendInviteLink(String toEmail, String token) {
        // no-op (token returned in invite creation response)
    }
}