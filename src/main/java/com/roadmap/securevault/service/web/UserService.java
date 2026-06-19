package com.roadmap.securevault.service.web;

import com.roadmap.securevault.config.KafkaTopics;
import com.roadmap.securevault.dto.keycloak.KeycloakUserQuery;
import com.roadmap.securevault.dto.keycloak.KeycloakUserRepresentation;
import com.roadmap.securevault.dto.web.UserFilter;
import com.roadmap.securevault.dto.web.UserUpdateRequest;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.exception.InvalidRoleOperationException;
import com.roadmap.securevault.exception.InvalidStateException;
import com.roadmap.securevault.exception.OwnerDeletionException;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakAuthClient keycloakAuthClient;
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    // if an app needs it
    public Page<KeycloakUserRepresentation> getUsers(Pageable pageable, UserFilter filter) {
        KeycloakUserQuery query = new KeycloakUserQuery(
                (int) pageable.getOffset(),
                pageable.getPageSize(),
                filter.username(),
                filter.email(),
                filter.enabled()
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

    public void addRole(UUID userId, RoleName role) {
        Set<String> currentRoles = keycloakAuthClient.getUserRealmRoles(userId);
        if (currentRoles.contains(role.name())) {
            throw new InvalidStateException("User already has role: " + role.name());
        }

        if (role == RoleName.ROLE_OWNER) {
            throw new InvalidRoleOperationException("Owner role cannot be assigned");
        }
        keycloakAuthClient.addRealmRole(userId, role);
    }

    public void removeRole(UUID userId, RoleName role) {
        Set<String> currentRoles = keycloakAuthClient.getUserRealmRoles(userId);
        if (!currentRoles.contains(role.name())) {
            throw new InvalidStateException("User does not have role: " + role.name());
        }

        if(role == RoleName.ROLE_OWNER && currentRoles
                .stream()
                .anyMatch(r -> r.equals(RoleName.ROLE_OWNER.name()))) {
            throw new InvalidRoleOperationException("Owner cannot be demoted");
        }

        if(currentRoles.size() == 1) {
            throw new InvalidRoleOperationException("Cannot remove last role from user");
        }
        keycloakAuthClient.removeRealmRole(userId, role);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        if(keycloakAuthClient.isOwner(userId))  // for isSelf
            throw new OwnerDeletionException("Owner cannot self-delete their account.");

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