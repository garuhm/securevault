package com.roadmap.securevault.service.web;

import com.roadmap.securevault.config.KafkaTopics;
import com.roadmap.securevault.dto.keycloak.KeycloakUserRepresentation;
import com.roadmap.securevault.dto.user.UserFilter;
import com.roadmap.securevault.dto.user.UserResponse;
import com.roadmap.securevault.dto.user.UserUpdateRequest;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.exception.InvalidRoleOperationException;
import com.roadmap.securevault.exception.InvalidStateException;
import com.roadmap.securevault.exception.OwnerDeletionException;
import com.roadmap.securevault.kafka.events.UserEvent;
import com.roadmap.securevault.mapper.UserMapper;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import com.roadmap.securevault.spec.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakAuthClient keycloakAuthClient;
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public Page<UserResponse> getUsers(Pageable pageable, UserFilter filter) {
        return userRepository.findAll(UserSpecification.fromFilter(filter), pageable)
                .map(UserMapper::toResponse);
    }

    public KeycloakUserRepresentation getUserById(UUID userId) {
        return keycloakAuthClient.getUserById(userId);
    }

    @Transactional
    public User getUserUsingClaims(Map<String, Object> claims) {
        UUID id = UUID.fromString((String) claims.get("sub"));

        return userRepository.findById(id)
                .orElseGet(() -> {
                    List<String> realmRoles = (List<String>) ((Map<String, Object>) claims.get("realm_access")).get("roles");

                    Set<RoleName> roles = realmRoles.stream()
                            .map(RoleName::tryParseRole)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    return userRepository.save(
                            User.builder()
                                    .id(id)
                                    .username((String) claims.get("preferred_username"))
                                    .email((String) claims.get("email"))
                                    .roles(roles)
                                    .build()
                    );
                });
    }

    @Transactional
    public KeycloakUserRepresentation updateUser(UUID userId, UserUpdateRequest request) {
        KeycloakUserRepresentation keycloakUser = keycloakAuthClient.updateUser(userId, request);

        UserEvent event = new UserEvent(
                userId,
                request.username(),
                request.email(),
                UserEvent.UserEventType.UPDATED,
                null
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
                UserEvent.UserEventType.UPDATED,
                null
        );
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.id().toString(), event);
        return keycloakUser;
    }

    public void addRole(UUID userId, Set<RoleName> roles) {
        Set<String> currentRoles = keycloakAuthClient.getUserCompositeRealmRoles(userId);
        roles.forEach(role -> {
            if (currentRoles.contains(role.name()))
                throw new InvalidStateException("User already has roles: " + role.name());

            if (role == RoleName.ROLE_OWNER)
                throw new InvalidRoleOperationException("Owner roles cannot be assigned");
        });

        Set<RoleName> rolesToAdd = roles.stream()
                .flatMap(role -> Stream.concat(
                        Stream.of(role),
                        RoleName.getRolesBelow(role).stream()
                ))
                .collect(Collectors.toSet());

        keycloakAuthClient.addRealmRoles(userId, rolesToAdd);

        UserEvent event = new UserEvent(
                userId,
                null,
                null,
                UserEvent.UserEventType.ROLE_ADDED,
                rolesToAdd
        );
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.id().toString(), event);
    }

    public void removeRole(UUID userId, Set<RoleName> roles) {
        Set<String> currentRoles = keycloakAuthClient.getUserCompositeRealmRoles(userId);
        roles.forEach(role -> {
            if (!currentRoles.contains(role.name())) {
                throw new InvalidStateException("User does not have roles: " + role.name());
            }

            if(role == RoleName.ROLE_OWNER && currentRoles
                    .stream()
                    .anyMatch(r -> r.equals(RoleName.ROLE_OWNER.name()))) {
                throw new InvalidRoleOperationException("Owner cannot be demoted");
            }

            if(role == RoleName.ROLE_USER)
                throw new InvalidRoleOperationException("Cannot remove user roles from user");
        });

        if(currentRoles.size() == 1) {
            throw new InvalidRoleOperationException("Cannot remove last roles from user");
        }
        keycloakAuthClient.removeRealmRole(userId, roles);

        UserEvent event = new UserEvent(
                userId,
                null,
                null,
                UserEvent.UserEventType.ROLE_REMOVED,
                roles
        );
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.id().toString(), event);
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
                    UserEvent.UserEventType.DELETED,
                    null
            );
            kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.id().toString(), event);
        }
    }
}