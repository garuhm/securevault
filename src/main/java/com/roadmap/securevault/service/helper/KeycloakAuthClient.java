package com.roadmap.securevault.service.helper;

import com.roadmap.securevault.config.properties.KeycloakProperties;
import com.roadmap.securevault.dto.KeycloakTokenResponse;
import com.roadmap.securevault.dto.KeycloakUserQuery;
import com.roadmap.securevault.dto.KeycloakUserRepresentation;
import com.roadmap.securevault.dto.RegisterRequest;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.exception.CredentialsTakenException;
import com.roadmap.securevault.exception.InvalidRefreshTokenException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")

@Service
@RequiredArgsConstructor
public class KeycloakAuthClient {

    private final KeycloakProperties keycloakProperties;
    private final RestClient restClient = RestClient.create();
    private final RedisCacheService redisCacheService;

    private static final String ADMIN_TOKEN_KEY = "keycloak:admin-token";
    private static final String USER_ROLES_KEY_PREFIX = "keycloak:user-roles:";
    private static final Duration ROLE_CACHE_TTL = Duration.ofMinutes(2);

    public List<KeycloakUserRepresentation> getAllUsers(KeycloakUserQuery query) {
        String adminToken = getAdminAccessToken();

        String uri = UriComponentsBuilder.fromUriString(keycloakProperties.adminUsersUri())
                .queryParamIfPresent("first", Optional.ofNullable(query.first()))
                .queryParamIfPresent("max", Optional.ofNullable(query.max()))
                .queryParamIfPresent("username", Optional.ofNullable(query.username()))
                .queryParamIfPresent("email", Optional.ofNullable(query.email()))
                .queryParamIfPresent("enabled", Optional.ofNullable(query.enabled()))
                .toUriString();

        List<Map<String, Object>> users = restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(List.class);

        return users.stream()
                .map(this::toUserRepresentation)
                .toList();
    }

    public long getUserCount(KeycloakUserQuery query) {
        String adminToken = getAdminAccessToken();

        String uri = UriComponentsBuilder.fromUriString(keycloakProperties.adminUserCountUri())
                .queryParamIfPresent("username", Optional.ofNullable(query.username()))
                .queryParamIfPresent("email", Optional.ofNullable(query.email()))
                .queryParamIfPresent("enabled", Optional.ofNullable(query.enabled()))
                .toUriString();

        Integer count = restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(Integer.class);

        return count;
    }

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

    public void createUser(RegisterRequest request) {
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
                )),
                "realmRoles", List.of(RoleName.ROLE_USER.name())
        );

        try {
            restClient.post()
                    .uri(keycloakProperties.adminUsersUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new CredentialsTakenException("Username or email already exists");
            }
            throw e;
        }
    }

    public KeycloakTokenResponse passwordGrantLogin(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProperties.clientId());
        form.add("client_secret", keycloakProperties.clientSecret());
        form.add("username", username);
        form.add("password", password);

        try {
            return requestTokens(form);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new BadCredentialsException("Username or password is incorrect.");
            }
            throw e;
        }
    }

    public KeycloakTokenResponse refreshGrant(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakProperties.clientId());
        form.add("client_secret", keycloakProperties.clientSecret());
        form.add("refresh_token", refreshToken);

        try {
            return requestTokens(form);
        } catch (RestClientResponseException e) {
            throw new InvalidRefreshTokenException("Refresh token is invalid, expired, or revoked.");
        }
    }

    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.clientId());
        form.add("client_secret", keycloakProperties.clientSecret());
        form.add("refresh_token", refreshToken);

        restClient.post()
                .uri(keycloakProperties.logoutUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
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

    private KeycloakTokenResponse requestTokens(MultiValueMap<String, String> form) {
        Map<String, Object> response = restClient.post()
                .uri(keycloakProperties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        return new KeycloakTokenResponse(
                (String) response.get("access_token"),
                (String) response.get("refresh_token"),
                ((Number) response.get("expires_in")).longValue(),
                ((Number) response.get("refresh_expires_in")).longValue()
        );
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

    private MultiValueMap<String, String> adminTokenForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", keycloakProperties.clientId());
        form.add("client_secret", keycloakProperties.clientSecret());
        return form;
    }

    private KeycloakUserRepresentation toUserRepresentation(Map<String, Object> user) {
        return new KeycloakUserRepresentation(
                UUID.fromString((String) user.get("id")),
                (String) user.get("username"),
                (String) user.get("email")
        );
    }
}