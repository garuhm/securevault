package com.roadmap.securevault.service.helper;

import com.roadmap.securevault.config.properties.KeycloakProperties;
import com.roadmap.securevault.dto.keycloak.KeycloakUserRepresentation;
import com.roadmap.securevault.dto.user.RegisterRequest;
import com.roadmap.securevault.dto.user.UserUpdateRequest;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.exception.CredentialsTakenException;
import com.roadmap.securevault.exception.InvalidStateException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class KeycloakAuthClient {

    private final KeycloakProperties keycloakProperties;
    private final RestClient restClient = RestClient.create();
    private final RedisCacheService redisCacheService;

    private static final String ADMIN_TOKEN_KEY = "keycloak:admin-token";
    private static final String USER_ROLES_KEY_PREFIX = "keycloak:user-roles:";
    private static final String USER_COMPOSITE_ROLES_KEY_PREFIX = "keycloak:user-composite-roles:";
    private static final Duration ROLE_CACHE_TTL = Duration.ofMinutes(2);


    public KeycloakUserRepresentation getUserById(UUID userId) {
        String adminToken = getAdminAccessToken();

        try {
            Map<String, Object> user = restClient.get()
                    .uri(keycloakProperties.adminUserByIdUri(userId.toString()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .body(Map.class);

            return toUserRepresentation(user);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new EntityNotFoundException("User not found: " + userId);
            }
            throw e;
        }
    }

    public boolean userExistsByEmail(String email) {
        String adminToken = getAdminAccessToken();

        String uri = UriComponentsBuilder.fromUriString(keycloakProperties.adminUsersUri())
                .queryParam("email", email)
                .queryParam("exact", true)
                .toUriString();

        List<Map<String, Object>> users = restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(List.class);

        return users != null && !users.isEmpty();
    }

    public boolean userExistsByUsername(String username) {
        String adminToken = getAdminAccessToken();

        String uri = UriComponentsBuilder.fromUriString(keycloakProperties.adminUsersUri())
                .queryParam("username", username)
                .queryParam("exact", true)
                .toUriString();

        List<Map<String, Object>> users = restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(List.class);

        return users != null && !users.isEmpty();
    }

    public Set<String> getUserRealmRoles(UUID userId) {
        String key = USER_ROLES_KEY_PREFIX + userId;

        return redisCacheService.get(key)
                .map(cached -> cached.isEmpty()
                        ? Set.<String>of()
                        : Set.copyOf(Arrays.asList(cached.split(","))))
                .orElseGet(() -> {
                    Set<String> roles = fetchUserRealmRolesFromKeycloak(userId);
                    redisCacheService.put(key, String.join(",", roles), ROLE_CACHE_TTL);
                    return roles;
                });
    }

    public Set<String> getUserCompositeRealmRoles(UUID userId) {
        String key = USER_COMPOSITE_ROLES_KEY_PREFIX + userId;

        return redisCacheService.get(key)
                .map(cached -> cached.isEmpty()
                        ? Set.<String>of()
                        : Set.copyOf(Arrays.asList(cached.split(","))))
                .orElseGet(() -> {
                    Set<String> roles = fetchUserCompositeRealmRolesFromKeycloak(userId);
                    redisCacheService.put(key, String.join(",", roles), ROLE_CACHE_TTL);
                    return roles;
                });
    }

    public void addRealmRole(UUID userId, RoleName role) {
        addRealmRoles(userId, Set.of(role));
    }

    public void addRealmRoles(UUID userId, Set<RoleName> roles) {
        String adminToken = getAdminAccessToken();

        List<RoleRepresentationDto> roleReps = roles.stream()
                .map(role -> fetchRealmRole(adminToken, role))
                .toList();

        try {
            restClient.post()
                    .uri(keycloakProperties.userRealmRoleMappingsUri(userId.toString()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(roleReps)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new EntityNotFoundException("User not found: " + userId);
            }
            throw e;
        }

        invalidateUserRolesCache(userId);
    }

    public void removeRealmRole(UUID userId, Set<RoleName> roles) {
        String adminToken = getAdminAccessToken();

        List<RoleRepresentationDto> roleReps = roles.stream()
                .map(role -> fetchRealmRole(adminToken, role))
                .collect(Collectors.toList());

        try {
            restClient.method(HttpMethod.DELETE)
                    .uri(keycloakProperties.userRealmRoleMappingsUri(userId.toString()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(roleReps)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new EntityNotFoundException("User not found: " + userId);
            }
            throw e;
        }

        invalidateUserRolesCache(userId);
    }

    public boolean isOwner(UUID userId) {
        Set<String> roles = getUserRealmRoles(userId);
        return roles
                .stream()
                .map(this::tryParseRole)
                .filter(Objects::nonNull)
                .anyMatch(role -> role == RoleName.ROLE_OWNER);
    }

    public UUID createUser(RegisterRequest request, RoleName role) {
        String adminToken = getAdminAccessToken();

        Map<String, Object> body = Map.of(
                "username", request.username(),
                "email", request.email(),
                "enabled", true,
                "emailVerified", true,
                "requiredActions", List.of(),
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.password(),
                        "temporary", false
                ))
        );

        UUID userId;
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(keycloakProperties.adminUsersUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            userId = extractIdFromLocation(response.getHeaders().getLocation());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new CredentialsTakenException("Username or email already exists");
            }
            throw e;
        }
        Set<RoleName> rolesToAdd = Stream.concat(
                Stream.of(role),
                RoleName.getRolesBelow(role).stream()
        ).collect(Collectors.toSet());

        addRealmRoles(userId, rolesToAdd);
        return userId;
    }

    public KeycloakUserRepresentation updateUser(UUID userId, UserUpdateRequest request) {
        String adminToken = getAdminAccessToken();

        Map<String, Object> body = new HashMap<>();
        body.put("id", userId.toString());
        body.put("username", request.username());
        body.put("email", request.email());
        body.put("enabled", true);
        body.put("emailVerified",true);

        putUserInternal(adminToken, userId, body);
        return toUserRepresentation(body);
    }

    public KeycloakUserRepresentation patchUser(UUID userId, UserUpdateRequest request) {
        String adminToken = getAdminAccessToken();

        Map<String, Object> existing = fetchRawUserById(adminToken, userId);

        if (request.username() != null) existing.put("username", request.username());
        if (request.email() != null) existing.put("email", request.email());

        putUserInternal(adminToken, userId, existing);
        return toUserRepresentation(existing);
    }

    public void deleteUser(UUID userId) {
        String adminToken = getAdminAccessToken();

        logoutAllSessions(userId);

        try {
            restClient.delete()
                    .uri(keycloakProperties.adminUserByIdUri(userId.toString()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new EntityNotFoundException("User not found: " + userId);
            }
            throw e;
        }
    }

    public void logoutAllSessions(UUID userId) {
        String adminToken = getAdminAccessToken();

        restClient.post()
                .uri(keycloakProperties.adminUserSessionsUri(userId.toString()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .toBodilessEntity();
    }

    // ---- internal helpers ----

    private RoleName tryParseRole(String authority) {
        try {
            return RoleName.valueOf(authority);
        } catch (IllegalArgumentException e) {
            return null; // not one of our roles (e.g. offline_access, uma_authorization)
        }
    }

    private void putUserInternal(String adminToken, UUID userId, Map<String, Object> body) {
        try {
            restClient.put()
                    .uri(keycloakProperties.adminUserByIdUri(userId.toString()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new EntityNotFoundException("User not found: " + userId);
            }
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new CredentialsTakenException("Username or email already exists");
            }
            throw e;
        }
    }

    private Map<String, Object> fetchRawUserById(String adminToken, UUID userId) {
        try {
            return restClient.get()
                    .uri(keycloakProperties.adminUserByIdUri(userId.toString()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new EntityNotFoundException("User not found: " + userId);
            }
            throw e;
        }
    }

    private UUID extractIdFromLocation(URI location) {
        if (location == null) {
            throw new InvalidStateException("Keycloak did not return a Location header for the created user");
        }
        String path = location.getPath();
        return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
    }

    private String getAdminAccessToken() {
        return redisCacheService.get(ADMIN_TOKEN_KEY).orElseGet(() -> {
            Map<String, Object> response = restClient.post()
                    .uri(keycloakProperties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(adminTokenForm())
                    .retrieve()
                    .body(Map.class);

            String token = (String) response.get("access_token");
            long expiresIn = ((Number) response.get("expires_in")).longValue();

            redisCacheService.put(ADMIN_TOKEN_KEY, token, Duration.ofSeconds(expiresIn - 10));
            return token;
        });
    }

    private Set<String> fetchUserRealmRolesFromKeycloak(UUID userId) {
        String adminToken = getAdminAccessToken();

        try {
            List<Map<String, Object>> roles = restClient.get()
                    .uri(keycloakProperties.userRealmRoleMappingsUri(userId.toString()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .body(List.class);

            return roles.stream()
                    .map(r -> (String) r.get("name"))
                    .collect(Collectors.toSet());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new EntityNotFoundException("User not found: " + userId);
            }
            throw e;
        }
    }

    private Set<String> fetchUserCompositeRealmRolesFromKeycloak(UUID userId) {
        String adminToken = getAdminAccessToken();

        try {
            List<Map<String, Object>> roles = restClient.get()
                    .uri(keycloakProperties.userRealmCompositeRoleMappingsUri(userId.toString()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .body(List.class);

            return roles.stream()
                    .map(r -> (String) r.get("name"))
                    .collect(Collectors.toSet());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new EntityNotFoundException("User not found: " + userId);
            }
            throw e;
        }
    }

    private RoleRepresentationDto fetchRealmRole(String adminToken, RoleName role) {
        try {
            Map<String, Object> roleMap = restClient.get()
                    .uri(keycloakProperties.realmRoleByNameUri(role.name()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .body(Map.class);

            return new RoleRepresentationDto(
                    (String) roleMap.get("id"),
                    (String) roleMap.get("name")
            );
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new EntityNotFoundException("Realm roles not found: " + role.name());
            }
            throw e;
        }
    }

    private void invalidateUserRolesCache( UUID userId) {
        String key = USER_ROLES_KEY_PREFIX + userId;
        redisCacheService.evict(key);
    }

    private MultiValueMap<String, String> adminTokenForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", keycloakProperties.serviceClientId());
        form.add("client_secret", keycloakProperties.serviceClientSecret());
        return form;
    }

    private KeycloakUserRepresentation toUserRepresentation(Map<String, Object> user) {
        UUID id = UUID.fromString((String) user.get("id"));

        Set<RoleName> roles = fetchUserRealmRolesFromKeycloak(id).stream()
                .map(this::tryParseRole)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        return new KeycloakUserRepresentation(
                UUID.fromString((String) user.get("id")),
                (String) user.get("username"),
                (String) user.get("email"),
                roles
        );
    }

    private record RoleRepresentationDto(String id, String name) {}
}