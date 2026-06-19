package com.roadmap.securevault.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.keycloak")
public record KeycloakProperties(
        String realm,
        String authServerUrl,
        String serviceClientId,
        String serviceClientSecret
) {
    public String tokenUri() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String introspectionUri() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";
    }

    public String adminUsersUri() {
        return authServerUrl + "/admin/realms/" + realm + "/users";
    }

    public String adminUserCountUri() {
        return authServerUrl + "/admin/realms/" + realm + "/users/count";
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

    public String userRealmCompositeRoleMappingsUri(String userId) {
        return authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm/composite";
    }


    public String realmRoleByNameUri(String roleName) {
        return authServerUrl + "/admin/realms/" + realm + "/roles/" + roleName;
    }
}