package com.roadmap.securevault.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ClaimAuthorityExtractor {

    private ClaimAuthorityExtractor() {}

    @SuppressWarnings("unchecked")
    public static Collection<GrantedAuthority> extract(Map<String, Object> claims) {
        Object realmAccess = claims.get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return List.of();
        }

        Object roles = realmAccessMap.get("roles");
        if (!(roles instanceof List<?> roleList)) {
            return List.of();
        }

        return roleList.stream()
                .map(String::valueOf)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}