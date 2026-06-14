package com.roadmap.securevault.security;

import com.roadmap.securevault.entity.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class UserAuthenticationToken extends AbstractAuthenticationToken {
    private final User user;
    private final Jwt jwt;

    public UserAuthenticationToken(User user, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.user = user;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override public Object getPrincipal() { return user; }
    @Override public Object getCredentials() { return jwt; }
}
