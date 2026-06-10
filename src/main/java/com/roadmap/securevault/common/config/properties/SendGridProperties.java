package com.roadmap.securevault.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

@ConfigurationProperties(prefix = "sendgrid")
@Profile("email")
public record SendGridProperties(
        String apiKey,
        String fromEmail
) {}