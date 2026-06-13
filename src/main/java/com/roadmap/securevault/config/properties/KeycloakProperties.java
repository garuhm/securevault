package com.roadmap.securevault.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.keycloak")
public record KeycloakProperties(
        String realm,
        String authServerUrl,
        String clientId,
        String clientSecret
) {
    public String tokenUri() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String logoutUri() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    public String adminUsersUri() {
        return authServerUrl + "/admin/realms/" + realm + "/users";
    }

    public String adminUserByIdUri(String userId) {
        return authServerUrl + "/admin/realms/" + realm + "/users/" + userId;
    }

    public String adminUserSessionsUri(String userId) {
        return authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/logout";
    }

    public String userRealmRoleMappingsUri(String userId) {
        return authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
    }
}