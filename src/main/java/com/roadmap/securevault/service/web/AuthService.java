package com.roadmap.securevault.service.web;

import com.roadmap.securevault.config.KafkaTopics;
import com.roadmap.securevault.dto.user.RegisterRequest;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.exception.CredentialsTakenException;
import com.roadmap.securevault.kafka.events.UserEvent;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final KeycloakAuthClient keycloakAuthClient;
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public void register(RegisterRequest credentials) {
        if (keycloakAuthClient.userExistsByUsername(credentials.username())) {
            throw new CredentialsTakenException("Username already exists");
        }
        if (keycloakAuthClient.userExistsByEmail(credentials.email())) {
            throw new CredentialsTakenException("Email already exists");
        }

        UUID userId = keycloakAuthClient.createUser(credentials, RoleName.ROLE_USER);

        UserEvent event = new UserEvent(
                userId,
                credentials.username(),
                credentials.email(),
                UserEvent.UserEventType.CREATED,
                Set.of(RoleName.ROLE_USER)
        );
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.id().toString(), event);
    }

    public void logoutAllSessions() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        keycloakAuthClient.logoutAllSessions(user.getId());
    }
}