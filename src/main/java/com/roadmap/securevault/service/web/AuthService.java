package com.roadmap.securevault.service.web;

import com.roadmap.securevault.config.KafkaTopics;
import com.roadmap.securevault.dto.web.RegisterRequest;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.kafka.events.UserEvent;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final KeycloakAuthClient keycloakAuthClient;
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public void register(RegisterRequest credentials) {
        UUID userId = keycloakAuthClient.createUser(credentials);

        UserEvent event = new UserEvent(
                userId,
                credentials.username(),
                credentials.email(),
                UserEvent.UserEventType.CREATED
        );
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.id().toString(), event);
    }

    public void logoutAllSessions() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        keycloakAuthClient.logoutAllSessions(user.getId());
    }
}