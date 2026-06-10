package com.roadmap.securevault.common.service;

import com.roadmap.securevault.common.config.properties.SendGridProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("email")
@RequiredArgsConstructor
public class SendGridNotificationService implements NotificationService {

    private final SendGridProperties sendGridProperties;

    @Override
    public void sendBootstrapLink(String toEmail, String token) {
        // use sendGridProperties.apiKey(), sendGridProperties.fromEmail()
    }

    @Override
    public void sendInviteLink(String toEmail, String token) {
        // use sendGridProperties.apiKey(), sendGridProperties.fromEmail()
    }
}