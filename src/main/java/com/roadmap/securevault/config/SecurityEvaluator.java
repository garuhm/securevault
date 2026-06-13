package com.roadmap.securevault.config;

import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.Role;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecurityEvaluator {

    private final UserRepository userRepository;

    public boolean isSelf(Authentication authentication, UUID targetUserId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        User currentUser = (User) authentication.getPrincipal();
        return currentUser.getId().equals(targetUserId);
    }

    // TODO: revisit once KeycloakAuthClient exists (step 8/9) — needs admin API role lookup for target user
    public boolean isAbove(Authentication authentication, UUID targetUserId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        Set<Role> currentRoles = authentication.getAuthorities()
                .stream()
                .map(Role.class::cast)
                .collect(Collectors.toSet());

        if (currentRoles.isEmpty()) return false;

        User targetUser = userRepository
                .findById(targetUserId)
                .orElse(null);

        if (targetUser == null) return false;

        int targetUserOrdinal = targetUser.getRoles()
                .stream()
                // get rolename enum from each role
                .map(Role::getName)
                .map(this::roleOrdinal)
                .max(Integer::compareTo)
                .orElse(2);

        int callerOrdinal = currentRoles.stream()
                .map(Role::getName)
                .map(this::roleOrdinal)
                .max(Integer::compareTo)
                .orElse(2);

        return callerOrdinal < targetUserOrdinal;
    }

    int roleOrdinal(RoleName role) {
        return switch (role) {
            case ROLE_OWNER -> 0;
            case ROLE_ADMIN -> 1;
            case ROLE_USER -> 2;
        };
    }
}