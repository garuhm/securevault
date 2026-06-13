package com.roadmap.securevault.service;

import com.roadmap.securevault.dto.KeycloakUserQuery;
import com.roadmap.securevault.dto.KeycloakUserRepresentation;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakAuthClient keycloakAuthClient;

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

    // TODO: WILL BE REWRITTEN TO LISTEN FOR KAFKA
    // INSTEAD OF DB-CALL-PER-UPDATE
    @Transactional
    public User getUserUsingJwt(Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());

        return userRepository.findById(id)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .id(id)
                                .username(jwt.getClaimAsString("preferred_username"))
                                .email(jwt.getClaimAsString("email"))
                                .build()
                ));
    }
}