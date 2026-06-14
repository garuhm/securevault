package com.roadmap.securevault.security;

import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.service.web.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserPrincipalJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserService userService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = userService.getUserUsingJwt(jwt); // unchanged - your JIT provisioning logic
        return new UserAuthenticationToken(user, jwt, extractAuthorities(jwt));
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return List.of();
        }

        List<String> roles = (List<String>) realmAccess.get("roles");

        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}