package com.roadmap.securevault.security;

import com.roadmap.securevault.entity.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.Collection;

public class UserAuthenticationToken extends AbstractAuthenticationToken {
    private final User user;
    private final OAuth2AuthenticatedPrincipal principal;

    public UserAuthenticationToken(User user, OAuth2AuthenticatedPrincipal principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.user = user;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override public Object getPrincipal() { return user; }
    @Override public Object getCredentials() { return principal; }
}
