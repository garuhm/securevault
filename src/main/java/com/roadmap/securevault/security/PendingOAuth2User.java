package com.roadmap.securevault.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class PendingOAuth2User implements OAuth2User {
    private final OAuth2User delegate;
    private final String email;
    private final String provider;
    private final String providerUserId;

    public PendingOAuth2User(OAuth2User delegate, String email, String provider, String providerUserId) {
        this.delegate = delegate;
        this.email = email;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}
