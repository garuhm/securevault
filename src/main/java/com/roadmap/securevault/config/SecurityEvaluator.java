package com.roadmap.securevault.config;

import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityEvaluator {

    private final UserRepository userRepository;
    private final KeycloakAuthClient keycloakAuthClient;

    public boolean isSelf(Authentication authentication, UUID targetUserId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        User currentUser = (User) authentication.getPrincipal();
        return currentUser.getId().equals(targetUserId);
    }

    public boolean isAbove(Authentication authentication, UUID targetUserId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        int callerOrdinal = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::tryParseRole)
                .filter(java.util.Objects::nonNull)
                .map(this::roleOrdinal)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);

        if (callerOrdinal == Integer.MAX_VALUE) return false;

        Set<String> targetRoleNames = keycloakAuthClient.getUserRealmRoles(targetUserId);

        int targetUserOrdinal = targetRoleNames.stream()
                .map(this::tryParseRole)
                .filter(java.util.Objects::nonNull)
                .map(this::roleOrdinal)
                .min(Integer::compareTo)
                .orElse(roleOrdinal(RoleName.ROLE_USER));

        return callerOrdinal < targetUserOrdinal;
    }

    private RoleName tryParseRole(String authority) {
        try {
            return RoleName.valueOf(authority);
        } catch (IllegalArgumentException e) {
            return null; // not one of our roles (e.g. offline_access, uma_authorization)
        }
    }

    int roleOrdinal(RoleName role) {
        return switch (role) {
            case ROLE_OWNER -> 0;
            case ROLE_ADMIN -> 1;
            case ROLE_USER -> 2;
        };
    }
}