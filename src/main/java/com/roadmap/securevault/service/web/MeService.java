package com.roadmap.securevault.service.web;

import com.roadmap.securevault.dto.web.MeResponse;
import com.roadmap.securevault.dto.web.MeRolesBelowResponse;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.mapper.UserMapper;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeService {
    private final KeycloakAuthClient keycloakAuthClient;

    public MeResponse getMe() {
        return UserMapper.toMeResponse((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    public MeRolesBelowResponse getRolesBelow() {
        RoleName roleName = RoleName.getHighestRole(
                SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                        .stream()
                        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                        .map(RoleName::tryParseRole)
                        .filter(r -> r != null)
                        .collect(Collectors.toSet()));

        return new MeRolesBelowResponse(RoleName.getRolesBelow(roleName));
    }
}
