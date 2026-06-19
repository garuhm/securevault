package com.roadmap.securevault.config;

import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityEvaluator {

    private final UserRepository userRepository;
    private final ApplicationContext applicationContext;
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
                .map(RoleName::tryParseRole)
                .filter(java.util.Objects::nonNull)
                .map(RoleName::getRoleHiearchyPosition)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);

        if (callerOrdinal == Integer.MAX_VALUE) return false;

        Set<String> targetRoleNames = keycloakAuthClient.getUserRealmRoles(targetUserId);

        int targetUserOrdinal = targetRoleNames.stream()
                .map(RoleName::tryParseRole)
                .filter(java.util.Objects::nonNull)
                .map(RoleName::getRoleHiearchyPosition)
                .min(Integer::compareTo)
                .orElse(RoleName.getRoleHiearchyPosition(RoleName.ROLE_USER));

        return callerOrdinal < targetUserOrdinal;
    }
}