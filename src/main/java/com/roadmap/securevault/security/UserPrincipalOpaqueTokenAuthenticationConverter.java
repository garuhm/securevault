package com.roadmap.securevault.security;

import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.service.web.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserPrincipalOpaqueTokenAuthenticationConverter implements OpaqueTokenAuthenticationConverter {
    private final UserService userService;

    @Override
    public Authentication convert(String introspectedToken, OAuth2AuthenticatedPrincipal principal) {
        Map<String, Object> claims = principal.getAttributes();

        User user = userService.getUserUsingClaims(claims);
        Collection<GrantedAuthority> authorities = ClaimAuthorityExtractor.extract(claims);

        return new UserAuthenticationToken(user, principal, authorities);
    }
}