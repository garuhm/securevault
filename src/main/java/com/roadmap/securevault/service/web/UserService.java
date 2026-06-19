package com.roadmap.securevault.service.web;

import com.roadmap.securevault.config.KafkaTopics;
import com.roadmap.securevault.dto.keycloak.KeycloakUserQuery;
import com.roadmap.securevault.dto.keycloak.KeycloakUserRepresentation;
import com.roadmap.securevault.dto.web.UserUpdateRequest;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.kafka.events.UserEvent;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakAuthClient keycloakAuthClient;
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    // if an app needs it
    public Page<KeycloakUserRepresentation> getUsers(Pageable pageable, String username, String email, Boolean enabled) {
        KeycloakUserQuery query = new KeycloakUserQuery(
                (int) pageable.getOffset(),
                pageable.getPageSize(),
                username,
                email,
                enabled
        );

        List<KeycloakUserRepresentation> content = keycloakAuthClient.getAllUsers(query);
        long total = keycloakAuthClient.getUserCount(query);

        return new PageImpl<>(content, pageable, total);
    }

    public KeycloakUserRepresentation getUserById(UUID userId) {
        return keycloakAuthClient.getUserById(userId);
    }

    @Transactional
    public User getUserUsingClaims(Map<String, Object> claims) {
        UUID id = UUID.fromString((String) claims.get("sub"));

        return userRepository.findById(id)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .id(id)
                                .username((String) claims.get("preferred_username"))
                                .email((String) claims.get("email"))
                                .build()
                ));
    }

    @Transactional
    public KeycloakUserRepresentation updateUser(UUID userId, UserUpdateRequest request) {
        KeycloakUserRepresentation keycloakUser = keycloakAuthClient.updateUser(userId, request);

        UserEvent event = new UserEvent(
                userId,
                request.username(),
                request.email(),
                UserEvent.UserEventType.UPDATED
        );
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.id().toString(), event);
        return keycloakUser;
    }
    
    @Transactional
    public KeycloakUserRepresentation partiallyUpdateUser(UUID userId, UserUpdateRequest request) {
        KeycloakUserRepresentation keycloakUser = keycloakAuthClient.patchUser(userId, request);

        UserEvent event = new UserEvent(
                userId,
                request.username(),
                request.email(),
                UserEvent.UserEventType.UPDATED
        );
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.id().toString(), event);
        return keycloakUser;
    }

    @Transactional
    public void deleteUser(UUID userId) {
        keycloakAuthClient.deleteUser(userId);

        if(userRepository.existsById(userId)) {
            UserEvent event = new UserEvent(
                    userId,
                    null,
                    null,
                    UserEvent.UserEventType.DELETED
            );
            kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.id().toString(), event);
        }
    }
}