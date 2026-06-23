package com.roadmap.securevault.controller;

import com.roadmap.securevault.dto.user.RegisterRequest;
import com.roadmap.securevault.dto.user.RoleUpdateRequest;
import com.roadmap.securevault.dto.user.UserUpdateRequest;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import com.roadmap.securevault.test_util.testcontainers.AbstractSpringBootTest;
import com.roadmap.securevault.util.ApiVersioningResolver;
import jakarta.persistence.EntityManager;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("UserController Integration Tests")
@AutoConfigureMockMvc
public class UserControllerIT extends AbstractSpringBootTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private KeycloakAuthClient keycloakAuthClient;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;

    private static final String GET_USERS_PATH = ApiVersioningResolver.resolve(UserController.class, "getUsers", "/users");
    private static final String GET_USER_PATH = ApiVersioningResolver.resolve(UserController.class, "getUser", "/users/{userId}");
    private static final String UPDATE_USER_PATH = ApiVersioningResolver.resolve(UserController.class, "updateUser", "/users/{userId}");
    private static final String PATCH_USER_PATH = ApiVersioningResolver.resolve(UserController.class, "patchUser", "/users/{userId}");
    private static final String DELETE_USER_PATH = ApiVersioningResolver.resolve(UserController.class, "deleteUser", "/users/{userId}");
    private static final String ADD_ROLE_PATH = ApiVersioningResolver.resolve(UserController.class, "patchUserRole", "/users/{userId}/roles");
    private static final String REMOVE_ROLE_PATH = ApiVersioningResolver.resolve(UserController.class, "deleteUserRole", "/users/{userId}/roles");

    private static List<String> SEED_USER_USERNAMES = List.of();
    private static List<String> SEED_ADMIN_USERNAMES = List.of();
    private static List<UUID> SEED_USER_IDS = List.of();
    private static List<UUID> SEED_ADMIN_IDS = List.of();
    private static final String SEED_USER_USERNAME_PREFIX = "user_";
    private static final String SEED_ADMIN_USERNAME_PREFIX = "admin_";
    private static final String SEED_EMAIL_SUFFIX = "@example.com";
    private static final String SEED_USER_PASSWORD = "User@1234";
    private static final String SEED_ADMIN_PASSWORD = "Admin@1234";

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
                    userRepository.deleteAllByUsernameIn(SEED_USER_USERNAMES);
                    userRepository.deleteAllByUsernameIn(SEED_ADMIN_USERNAMES);
        });

        SEED_USER_USERNAMES =
                IntStream.range(0, 25)
                .mapToObj(i -> SEED_USER_USERNAME_PREFIX
                        + i + "_"
                        + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16))
                .toList();
        SEED_ADMIN_USERNAMES =
                IntStream.range(0, 2)
                .mapToObj(i -> 
                        SEED_ADMIN_USERNAME_PREFIX
                                + i + "_"
                                + UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 16))
                .toList();
        
        List<RegisterRequest> seedUserRegisterRequests = SEED_USER_USERNAMES.stream()
                .map(username -> new RegisterRequest(
                        username,
                        username + SEED_EMAIL_SUFFIX,
                        SEED_USER_PASSWORD))
                .toList();
        List<RegisterRequest> seedAdminRegisterRequests = SEED_ADMIN_USERNAMES.stream()
                .map(username -> new RegisterRequest(
                        username,
                        username + SEED_EMAIL_SUFFIX,
                        SEED_ADMIN_PASSWORD))
                .toList();
        

        List<User> seedUsers = seedUserRegisterRequests.stream()
                .map(registerRequest -> User.builder()
                        .id(keycloakAuthClient.createUser(registerRequest, RoleName.ROLE_USER))
                        .username(registerRequest.username())
                        .email(registerRequest.email())
                        .roles(Set.of(RoleName.ROLE_USER))
                        .build())
                .toList();
        SEED_USER_IDS = seedUsers.stream().map(User::getId).toList();

        List<User> seedAdmins = seedAdminRegisterRequests.stream()
                .map(registerRequest -> User.builder()
                        .id(keycloakAuthClient.createUser(registerRequest, RoleName.ROLE_ADMIN))
                        .username(registerRequest.username())
                        .email(registerRequest.email())
                        .roles(RoleName.combineRoles(
                                Set.of(RoleName.ROLE_ADMIN),
                                RoleName.getRolesBelow(RoleName.ROLE_ADMIN)
                        ))
                        .build())
                .toList();
        SEED_ADMIN_IDS = seedAdmins.stream().map(User::getId).toList();

        transactionTemplate.executeWithoutResult(status -> {
                    userRepository.saveAll(seedUsers);
                    userRepository.saveAll(seedAdmins);
                    entityManager.flush();
                    entityManager.clear();
        });
    }

    @Nested
    @DisplayName("Get users tests")
    class GetUsers {
        String accessToken = obtainAccessToken("app_owner", "Owner@1234");

        @Test
        @DisplayName("Get users while unauthenticated; 401 status code")
        void getUsersWhileUnauthenticated() throws Exception {
            mockMvc.perform(get(GET_USERS_PATH))
                    .andExpect(status().isUnauthorized());
        }

        @Nested
        @DisplayName("Pagination and sorting tests; 200 status codes")
        class PaginationAndSorting {

            @Test
            @DisplayName("Get users page 0 size 10 sorted by username asc; 10 results total 28")
            void getUsersFirstPage() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "username,asc"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(10)))
                        .andExpect(jsonPath("$.totalElements").value(28))
                        .andExpect(jsonPath("$.totalPages").value(3));
            }

            @Test
            @DisplayName("Get users page 2 size 10; 8 remainder results total 28")
            void getUsersLastPage() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("page", "2")
                                .param("size", "10")
                                .param("sort", "username,asc"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(8)))
                        .andExpect(jsonPath("$.totalElements").value(28))
                        .andExpect(jsonPath("$.totalPages").value(3));
            }
        }

        @Nested
        @DisplayName("Username filter tests; 200 status codes")
        class UsernameFilter {

            @Test
            @DisplayName("Filter by username 'user_'; matches 25 regular users")
            void filterByUserPrefix() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("username", "user_"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(20)))
                        .andExpect(jsonPath("$.totalElements").value(25))
                        .andExpect(jsonPath("$.totalPages").value(2));
            }

            @Test
            @DisplayName("Filter by username 'admin_'; matches 2 admins")
            void filterByAdminPrefix() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("username", "admin_"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(2)))
                        .andExpect(jsonPath("$.totalElements").value(2))
                        .andExpect(jsonPath("$.totalPages").value(1));
            }

            @Test
            @DisplayName("Filter by username 'app_owner'; matches owner only")
            void filterByOwnerUsername() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("username", "app_owner"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(1)))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.totalPages").value(1));
            }

            @Test
            @DisplayName("Filter by username with no match; 0 results")
            void filterByUsernameNoMatch() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("username", "zzznomatch"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(0)))
                        .andExpect(jsonPath("$.totalElements").value(0))
                        .andExpect(jsonPath("$.totalPages").value(0));
            }
        }

        @Nested
        @DisplayName("Email filter tests; 200 status codes")
        class EmailFilter {

            @Test
            @DisplayName("Filter by email 'example.com'; matches 27 users excluding owner")
            void filterByExampleComEmail() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("email", "example.com"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(20)))
                        .andExpect(jsonPath("$.totalElements").value(27))
                        .andExpect(jsonPath("$.totalPages").value(2));
            }

            @Test
            @DisplayName("Filter by email 'app.local'; matches owner only")
            void filterByAppLocalEmail() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("email", "app.local"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(1)))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.totalPages").value(1));
            }
        }

        @Nested
        @DisplayName("Include roles filter tests; 200 status codes")
        class IncludeRolesFilter {

            @Test
            @DisplayName("Include ROLE_OWNER; matches owner only")
            void includeOwnerRole() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("includeRoles", Set.of(RoleName.ROLE_OWNER).stream()
                                        .map(RoleName::name).toArray(String[]::new)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(1)))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.totalPages").value(1));
            }

            @Test
            @DisplayName("Include ROLE_ADMIN; matches owner and 2 admins")
            void includeAdminRole() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("includeRoles", Set.of(RoleName.ROLE_ADMIN).stream()
                                        .map(RoleName::name).toArray(String[]::new)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(3)))
                        .andExpect(jsonPath("$.totalElements").value(3))
                        .andExpect(jsonPath("$.totalPages").value(1));
            }

            @Test
            @DisplayName("Include ROLE_USER; matches all 28 users")
            void includeUserRole() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("includeRoles", Set.of(RoleName.ROLE_USER).stream()
                                        .map(RoleName::name).toArray(String[]::new)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(20)))
                        .andExpect(jsonPath("$.totalElements").value(28))
                        .andExpect(jsonPath("$.totalPages").value(2));
            }

            @Test
            @DisplayName("Include ROLE_ADMIN and ROLE_USER; matches all 28 users")
            void includeAdminAndUserRoles() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("includeRoles", Set.of(RoleName.ROLE_ADMIN, RoleName.ROLE_USER).stream()
                                        .map(RoleName::name).toArray(String[]::new)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(20)))
                        .andExpect(jsonPath("$.totalElements").value(28))
                        .andExpect(jsonPath("$.totalPages").value(2));
            }
        }

        @Nested
        @DisplayName("Exclude roles filter tests; 200 status codes")
        class ExcludeRolesFilter {

            @Test
            @DisplayName("Exclude ROLE_OWNER; matches everyone except owner")
            void excludeOwnerRole() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("excludeRoles", Set.of(RoleName.ROLE_OWNER).stream()
                                        .map(RoleName::name).toArray(String[]::new)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(20)))
                        .andExpect(jsonPath("$.totalElements").value(27))
                        .andExpect(jsonPath("$.totalPages").value(2));
            }

            @Test
            @DisplayName("Exclude ROLE_ADMIN; matches 25 plain users only")
            void excludeAdminRole() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("excludeRoles", Set.of(RoleName.ROLE_ADMIN).stream()
                                        .map(RoleName::name).toArray(String[]::new)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(20)))
                        .andExpect(jsonPath("$.totalElements").value(25))
                        .andExpect(jsonPath("$.totalPages").value(2));
            }

            @Test
            @DisplayName("Exclude ROLE_USER; matches nobody")
            void excludeUserRole() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("excludeRoles", Set.of(RoleName.ROLE_USER).stream()
                                        .map(RoleName::name).toArray(String[]::new)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(0)))
                        .andExpect(jsonPath("$.totalElements").value(0))
                        .andExpect(jsonPath("$.totalPages").value(0));
            }
        }

        @Nested
        @DisplayName("Combined filter tests; 200 status codes")
        class CombinedFilter {

            @Test
            @DisplayName("Filter by admin username with include ROLE_ADMIN page size 1; 1 result total 2")
            void filterAdminsByUsernameAndRolePaged() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("page", "0")
                                .param("size", "1")
                                .param("sort", "username,asc")
                                .param("username", "admin_")
                                .param("includeRoles", Set.of(RoleName.ROLE_ADMIN).stream()
                                        .map(RoleName::name).toArray(String[]::new)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(1)))
                        .andExpect(jsonPath("$.totalElements").value(2))
                        .andExpect(jsonPath("$.totalPages").value(2));
            }

            @Test
            @DisplayName("Filter by user username with exclude ROLE_ADMIN page size 10; 10 results total 25")
            void filterUsersByUsernameExcludingAdminsPaged() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "email,asc")
                                .param("username", "user_")
                                .param("excludeRoles", Set.of(RoleName.ROLE_ADMIN).stream()
                                        .map(RoleName::name).toArray(String[]::new)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(10)))
                        .andExpect(jsonPath("$.totalElements").value(25))
                        .andExpect(jsonPath("$.totalPages").value(3));
            }

            @Test
            @DisplayName("Filter by owner email with include ROLE_OWNER; matches owner only")
            void filterOwnerByEmailAndRole() throws Exception {
                mockMvc.perform(get(GET_USERS_PATH)
                                .header("Authorization", "Bearer " + accessToken)
                                .param("email", "app.local")
                                .param("includeRoles", Set.of(RoleName.ROLE_OWNER).stream()
                                        .map(RoleName::name).toArray(String[]::new)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").exists())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content", hasSize(1)))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.totalPages").value(1));
            }
        }
    }

    @Nested
    @DisplayName("Get user tests")
    class GetUser {
        String accessToken = obtainAccessToken("app_owner", "Owner@1234");

        @Test
        @DisplayName("Get existent user; 200 status code")
        void getExistentUser() throws Exception {
            mockMvc.perform(get(GET_USER_PATH, SEED_ADMIN_IDS.getFirst())
                                .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SEED_ADMIN_IDS.getFirst().toString()))
                    .andExpect(jsonPath("$.username").value(SEED_ADMIN_USERNAMES.getFirst()))
                    .andExpect(jsonPath("$.email").value(SEED_ADMIN_USERNAMES.getFirst() + SEED_EMAIL_SUFFIX))
                    .andExpect(jsonPath("$.roles").isArray())
                    .andExpect(jsonPath("$.roles", hasSize(2)))
                    .andExpect(jsonPath("$.roles").value(hasItem(RoleName.ROLE_ADMIN.name())))
                    .andExpect(jsonPath("$.roles").value(hasItem(RoleName.ROLE_USER.name())));
        }

        @Test
        @DisplayName("Get non-existent user; 404 status code")
        void getNonExistentUser() throws Exception {
            mockMvc.perform(get(GET_USER_PATH, UUID.randomUUID())
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Get user while unauthenticated; 401 status code")
        void getUserWhileUnauthenticated() throws Exception {
            mockMvc.perform(get(GET_USER_PATH, UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Update user tests")
    class UpdateUser {
        String ownerAccessToken = obtainAccessToken("app_owner", "Owner@1234");

        @Test
        @DisplayName("Update user with right fields as owner; 200 status code")
        void updateUserWithRightFieldsAsOwner() throws Exception {
            String uniqueUUID = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            UserUpdateRequest request = new UserUpdateRequest(
                    "new_username" + uniqueUUID,
                    "new_email" + uniqueUUID + SEED_EMAIL_SUFFIX);

            mockMvc.perform(put(UPDATE_USER_PATH, SEED_USER_IDS.getFirst())
                                .header("Authorization", "Bearer " + ownerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() ->
                            userRepository.findById(SEED_USER_IDS.getFirst())
                                    .ifPresent(user -> {
                                        assertEquals("new_username" + uniqueUUID, user.getUsername());
                                        assertEquals("new_email" + uniqueUUID + SEED_EMAIL_SUFFIX, user.getEmail());
            }));
        }

        @Test
        @DisplayName("Update user with right fields as admin; 200 status code")
        void updateUserWithRightFieldsAsAdmin() throws Exception {
            String adminAccessToken = obtainAccessToken(SEED_ADMIN_USERNAMES.getFirst(), "Admin@1234");

            String uniqueUUID = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            UserUpdateRequest request = new UserUpdateRequest(
                    "new_username" + uniqueUUID,
                    "new_email" + uniqueUUID + SEED_EMAIL_SUFFIX);

            mockMvc.perform(put(UPDATE_USER_PATH, SEED_USER_IDS.getFirst())
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.username").value("new_username" + uniqueUUID))
                        .andExpect(jsonPath("$.email").value("new_email" + uniqueUUID + SEED_EMAIL_SUFFIX));

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() ->
                            userRepository.findById(SEED_USER_IDS.getFirst())
                                    .ifPresent(user -> {
                                        assertEquals("new_username" + uniqueUUID, user.getUsername());
                                        assertEquals("new_email" + uniqueUUID + SEED_EMAIL_SUFFIX, user.getEmail());
                                    })
                    );
        }

        @Test
        @DisplayName("Update user as self; 200 status code")
        void updateUserAsSelf() throws Exception {
            String userAccessToken = obtainAccessToken(SEED_USER_USERNAMES.getFirst(), "User@1234");

            String uniqueUUID = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            UserUpdateRequest request = new UserUpdateRequest(
                    null,
                    "new_email" + uniqueUUID + SEED_EMAIL_SUFFIX);

            mockMvc.perform(patch(PATCH_USER_PATH, SEED_USER_IDS.getFirst())
                                .header("Authorization", "Bearer " + userAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Update user with invalid fields; 400 status code")
        void updateUserWithInvalidFields() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest("new", "new_email123");

            mockMvc.perform(put(UPDATE_USER_PATH, SEED_USER_IDS.getFirst())
                                .header("Authorization", "Bearer " + ownerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Update nonexistent user; 404 status code")
        void updateNonExistentUser() throws Exception {
            String uniqueUUID = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            UserUpdateRequest request = new UserUpdateRequest(
                    "new_username" + uniqueUUID,
                    "new_email" + uniqueUUID + SEED_EMAIL_SUFFIX);

            mockMvc.perform(put(UPDATE_USER_PATH, UUID.randomUUID())
                                .header("Authorization", "Bearer " + ownerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Update owner as admin; 403 status code")
        void updateOwnerAsAdmin() throws Exception {
            // load owner in db if not there yet
            mockMvc.perform(
                            get(ApiVersioningResolver.resolve(MeController.class, "me", "/me"))
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAccessToken));

            String adminAccessToken = obtainAccessToken(SEED_ADMIN_USERNAMES.getFirst(), "Admin@1234");
            UUID ownerId = userRepository.findByUsername("app_owner").get().getId();

            String uniqueUUID = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            UserUpdateRequest request = new UserUpdateRequest(
                    "new_username" + uniqueUUID,
                    "new_email" + uniqueUUID + SEED_EMAIL_SUFFIX);

            mockMvc.perform(put(UPDATE_USER_PATH, ownerId)
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Update as a user; 403 status code")
        void updateAsUser() throws Exception {
            String userAccessToken = obtainAccessToken(SEED_USER_USERNAMES.getFirst(), "User@1234");

            String uniqueUUID = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            UserUpdateRequest request = new UserUpdateRequest(
                    "new_username" + uniqueUUID,
                    "new_email" + uniqueUUID + SEED_EMAIL_SUFFIX);

            mockMvc.perform(put(UPDATE_USER_PATH, SEED_USER_IDS.get(2))
                                .header("Authorization", "Bearer " + userAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Update user while unauthenticated; 401 status code")
        void updateUserWhileUnauthenticated() throws Exception {
            String uniqueUUID = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            UserUpdateRequest request = new UserUpdateRequest(
                    "new_username" + uniqueUUID,
                    "new_email" + uniqueUUID + SEED_EMAIL_SUFFIX);

            mockMvc.perform(put(UPDATE_USER_PATH, SEED_USER_IDS.getFirst())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Patch user tests")
    class PatchUser {
        String ownerAccessToken = obtainAccessToken("app_owner", "Owner@1234");

        @Test
        @DisplayName("Patch user with right field(s) as owner; 200 status code")
        void patchUserWithRightFields() throws Exception {
            String uniqueUUID1 = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String uniqueUUID2 = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            UserUpdateRequest request1 = new UserUpdateRequest(
                    "new_username" + uniqueUUID1,
                    "new_email" + uniqueUUID1 + SEED_EMAIL_SUFFIX);

            UserUpdateRequest request2 = new UserUpdateRequest(
                    "new_username" + uniqueUUID2,
                    null);

            mockMvc.perform(patch(UPDATE_USER_PATH, SEED_USER_IDS.getFirst())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request1))
                                .header("Authorization", "Bearer " + ownerAccessToken))
                        .andExpect(status().isOk());

            mockMvc.perform(patch(UPDATE_USER_PATH, SEED_USER_IDS.get(2))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request2))
                                .header("Authorization", "Bearer " + ownerAccessToken))
                        .andExpect(status().isOk());

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        userRepository.findById(SEED_USER_IDS.getFirst())
                                .ifPresent(user -> {
                                    assertEquals("new_username" + uniqueUUID1, user.getUsername());
                                    assertEquals("new_email" + uniqueUUID1 + SEED_EMAIL_SUFFIX, user.getEmail());
                                });
                        userRepository.findById(SEED_USER_IDS.get(2))
                                .ifPresent(user -> {
                                    assertEquals("new_username" + uniqueUUID2, user.getUsername());
                                    assertEquals(SEED_USER_USERNAMES.get(2) + SEED_EMAIL_SUFFIX, user.getEmail());
                                });
                    });

        }

        @Test
        @DisplayName("Patch user w the right field(s) as admin; 200 status code")
        void patchUserWithRightFieldsAsAdmin() throws Exception {
            String adminAccessToken = obtainAccessToken(SEED_ADMIN_USERNAMES.getFirst(), "Admin@1234");

            String uniqueUUID1 = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String uniqueUUID2 = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

                        UserUpdateRequest request1 = new UserUpdateRequest(
                    "new_username" + uniqueUUID1,
                    "new_email" + uniqueUUID1 + SEED_EMAIL_SUFFIX);

            UserUpdateRequest request2 = new UserUpdateRequest(
                    "new_username" + uniqueUUID2,
                    null);

            mockMvc.perform(patch(UPDATE_USER_PATH, SEED_USER_IDS.getFirst())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request1))
                                .header("Authorization", "Bearer " + ownerAccessToken))
                        .andExpect(status().isOk());

            mockMvc.perform(patch(UPDATE_USER_PATH, SEED_USER_IDS.get(2))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request2))
                                .header("Authorization", "Bearer " + ownerAccessToken))
                        .andExpect(status().isOk());

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        userRepository.findById(SEED_USER_IDS.getFirst())
                                .ifPresent(user -> {
                                    assertEquals("new_username" + uniqueUUID1, user.getUsername());
                                    assertEquals("new_email" + uniqueUUID1 + SEED_EMAIL_SUFFIX, user.getEmail());
                                });
                        userRepository.findById(SEED_USER_IDS.get(2))
                                .ifPresent(user -> {
                                    assertEquals("new_username" + uniqueUUID2, user.getUsername());
                                    assertEquals(SEED_USER_USERNAMES.get(2) + SEED_EMAIL_SUFFIX, user.getEmail());
                                });
                    });
        }

        @Test
        @DisplayName("Patch user with invalid fields; 400 status code")
        void patchUserWithInvalidFields() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest(null, "new_email123");

            mockMvc.perform(patch(PATCH_USER_PATH, SEED_USER_IDS.getFirst())
                                .header("Authorization", "Bearer " + ownerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Patch nonexistent user; 404 status code")
        void patchNonExistentUser() throws Exception {
            String uniqueUUID = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            UserUpdateRequest request = new UserUpdateRequest(
                    null,
                    "new_email" + uniqueUUID + SEED_EMAIL_SUFFIX);

            mockMvc.perform(patch(PATCH_USER_PATH, UUID.randomUUID())
                                .header("Authorization", "Bearer " + ownerAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Patch as a user; 403 status code")
        void patchAsUser() throws Exception {
            String userAccessToken = obtainAccessToken(SEED_USER_USERNAMES.getFirst(), "User@1234");

            String uniqueUUID = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            UserUpdateRequest request = new UserUpdateRequest(
                    null,
                    "new_email" + uniqueUUID + SEED_EMAIL_SUFFIX);

            mockMvc.perform(patch(PATCH_USER_PATH, SEED_USER_IDS.get(2))
                                .header("Authorization", "Bearer " + userAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Patch user while unauthenticated; 401 status code")
        void patchUserWhileUnauthenticated() throws Exception {
            String uniqueUUID = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            UserUpdateRequest request = new UserUpdateRequest(
                    "new_username" + uniqueUUID,
                    null);

            mockMvc.perform(patch(PATCH_USER_PATH, SEED_USER_IDS.getFirst())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Delete user tests")
    class DeleteUser {
        String ownerAccessToken = obtainAccessToken("app_owner", "Owner@1234");

        @Test
        @DisplayName("Delete admin as owner; 204 status code")
        void deleteAdminAsOwner() throws Exception {
            mockMvc.perform(delete(DELETE_USER_PATH, SEED_ADMIN_IDS.getFirst())
                            .header("Authorization", "Bearer " + ownerAccessToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Delete user as owner; 204 status code")
        void deleteUserAsOwner() throws Exception {
            mockMvc.perform(delete(DELETE_USER_PATH, SEED_USER_IDS.getFirst())
                            .header("Authorization", "Bearer " + ownerAccessToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Delete user as admin; 204 status code")
        void deleteUserAsAdmin() throws Exception {
            String adminAccessToken = obtainAccessToken(SEED_ADMIN_USERNAMES.getFirst(), SEED_ADMIN_PASSWORD);

            mockMvc.perform(delete(DELETE_USER_PATH, SEED_USER_IDS.getFirst())
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Delete self as admin; 204 status code")
        void deleteSelfAsAdmin() throws Exception {
            String adminAccessToken = obtainAccessToken(SEED_ADMIN_USERNAMES.getFirst(), SEED_ADMIN_PASSWORD);

            mockMvc.perform(delete(DELETE_USER_PATH, SEED_ADMIN_IDS.getFirst())
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Delete self as user; 204 status code")
        void deleteSelfAsUser() throws Exception {
            String userAccessToken = obtainAccessToken(SEED_USER_USERNAMES.getFirst(), SEED_USER_PASSWORD);

            mockMvc.perform(delete(DELETE_USER_PATH, SEED_USER_IDS.getFirst())
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Delete owner as admin; 403 status code")
        void deleteOwnerAsAdmin() throws Exception {
            mockMvc.perform(
                    get(ApiVersioningResolver.resolve(MeController.class, "me", "/me"))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAccessToken));

            String adminAccessToken = obtainAccessToken(SEED_ADMIN_USERNAMES.getFirst(), SEED_ADMIN_PASSWORD);
            UUID ownerId = userRepository.findByUsername("app_owner").get().getId();

            mockMvc.perform(delete(DELETE_USER_PATH, ownerId)
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Delete self as owner; 403 status code")
        void deleteSelfAsOwner() throws Exception {
            mockMvc.perform(
                    get(ApiVersioningResolver.resolve(MeController.class, "me", "/me"))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAccessToken));

            UUID ownerId = userRepository.findByUsername("app_owner").get().getId();

            mockMvc.perform(delete(DELETE_USER_PATH, ownerId)
                            .header("Authorization", "Bearer " + ownerAccessToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Delete user as a user; 403 status code")
        void deleteUserAsUser() throws Exception {
            String userAccessToken = obtainAccessToken(SEED_USER_USERNAMES.getFirst(), SEED_USER_PASSWORD);

            mockMvc.perform(delete(DELETE_USER_PATH, SEED_USER_IDS.get(2))
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Delete nonexistent user; 404 status code")
        void deleteNonExistentUser() throws Exception {
            mockMvc.perform(delete(DELETE_USER_PATH, UUID.randomUUID())
                            .header("Authorization", "Bearer " + ownerAccessToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Delete user while unauthenticated; 401 status code")
        void deleteUserWhileUnauthenticated() throws Exception {
            mockMvc.perform(delete(DELETE_USER_PATH, SEED_USER_IDS.getFirst()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Add roles tests")
    class AddRole {
        String ownerAccessToken = obtainAccessToken("app_owner", "Owner@1234");

        @Test
        @DisplayName("Add admin roles as owner; 204 status code")
        void addAdminRoleAsOwner() throws Exception {
            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_ADMIN));
            mockMvc.perform(patch(ADD_ROLE_PATH, SEED_USER_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + ownerAccessToken)
                    )
                    .andExpect(status().isNoContent());

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> transactionTemplate.executeWithoutResult(status -> {
                        userRepository.findById(SEED_USER_IDS.getFirst())
                                .ifPresent(user -> {
                                    assertTrue(user.getRoles().contains(RoleName.ROLE_ADMIN));
                                });
                    }));
        }

        @Test
        @DisplayName("Add roles already present; 409 status code")
        void addRoleAlreadyPresent() throws Exception {
            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_ADMIN));
            mockMvc.perform(patch(ADD_ROLE_PATH, SEED_ADMIN_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + ownerAccessToken)
                    )
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Add owner role; 409 status code")
        void addOwnerRole() throws Exception {
            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_OWNER));
            mockMvc.perform(patch(ADD_ROLE_PATH, SEED_ADMIN_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + ownerAccessToken)
                    )
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Add nonexistent roles; 400 status code")
        void addNonExistentRole() throws Exception {
            String nonExistentRoles = "{\"roles\": [\"ROLE_NONEXISTENT\"]}";
            mockMvc.perform(patch(ADD_ROLE_PATH, SEED_ADMIN_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(nonExistentRoles)
                            .header("Authorization", "Bearer " + ownerAccessToken)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Add roles to nonexistent user; 404 status code")
        void addRoleToNonExistentUser() throws Exception {
            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_ADMIN));
            mockMvc.perform(patch(ADD_ROLE_PATH, UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + ownerAccessToken)
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Add roles as admin; 403 status code")
        void addRoleAsAdmin() throws Exception {
            String adminAccessToken = obtainAccessToken(SEED_ADMIN_USERNAMES.getFirst(), "Admin@1234");

            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_ADMIN));
            mockMvc.perform(patch(ADD_ROLE_PATH, SEED_USER_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + adminAccessToken)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Add roles as user; 403 status code")
        void addRoleAsUser() throws Exception {
            String userAccessToken = obtainAccessToken(SEED_USER_USERNAMES.getFirst(), "User@1234");

            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_ADMIN));
            mockMvc.perform(patch(ADD_ROLE_PATH, SEED_USER_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + userAccessToken)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Add role while unauthenticated; 401 status code")
        void addRoleWhileUnauthenticated() throws Exception {
            mockMvc.perform(patch(ADD_ROLE_PATH, SEED_USER_IDS.getFirst()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Remove role tests")
    class RemoveRole {
        String ownerAccessToken = obtainAccessToken("app_owner", "Owner@1234");

        @Test
        @DisplayName("Remove admin role; 204 status code")
        void removeAdminRole() throws Exception {
            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_ADMIN));
            mockMvc.perform(delete(REMOVE_ROLE_PATH, SEED_ADMIN_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + ownerAccessToken)
                    )
                    .andExpect(status().isNoContent());

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> transactionTemplate.executeWithoutResult(status -> {
                        userRepository.findById(SEED_USER_IDS.getFirst())
                                .ifPresent(user -> {
                                    assertFalse(user.getRoles().contains(RoleName.ROLE_ADMIN));
                                });
                    }));
        }

        @Test
        @DisplayName("Remove role not already present; 409 status code")
        void removeRoleNotAlreadyPresent() throws Exception {
            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_ADMIN));
            mockMvc.perform(delete(REMOVE_ROLE_PATH, SEED_USER_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + ownerAccessToken)
                    )
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Remove user role; 403 status code")
        void removeUserRole() throws Exception {
            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_USER));
            mockMvc.perform(delete(REMOVE_ROLE_PATH, SEED_ADMIN_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + ownerAccessToken)
                    )
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Remove owner role; 409 status code")
        void removeOwnerRole() throws Exception {
            mockMvc.perform(
                    get(ApiVersioningResolver.resolve(MeController.class, "me", "/me"))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAccessToken));

            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_OWNER));
            UUID ownerId = userRepository.findByUsername("app_owner").get().getId();

            mockMvc.perform(delete(REMOVE_ROLE_PATH, ownerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + ownerAccessToken)
                    )
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Remove nonexistent roles; 400 status code")
        void removeNonExistentRole() throws Exception {
            String nonExistentRoles = "{\"roles\": [\"ROLE_NONEXISTENT\"]}";
            mockMvc.perform(delete(REMOVE_ROLE_PATH, SEED_ADMIN_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(nonExistentRoles)
                            .header("Authorization", "Bearer " + ownerAccessToken)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Remove role from nonexistent user; 404 status code")
        void removeRoleFromNonExistentUser() throws Exception {
            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_ADMIN));
            mockMvc.perform(delete(REMOVE_ROLE_PATH, UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + ownerAccessToken)
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Remove role as admin; 403 status code")
        void removeRoleAsAdmin() throws Exception {
            String adminAccessToken = obtainAccessToken(SEED_ADMIN_USERNAMES.getFirst(), "Admin@1234");
            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_USER));

            mockMvc.perform(delete(REMOVE_ROLE_PATH, SEED_USER_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + adminAccessToken)
                    )
                    .andExpect(status().isForbidden());
        }
        @Test
        @DisplayName("Remove role as user; 403 status code")
        void removeRoleAsUser() throws Exception {
            String userAccessToken = obtainAccessToken(SEED_USER_USERNAMES.getFirst(), "User@1234");
            RoleUpdateRequest request = new RoleUpdateRequest(Set.of(RoleName.ROLE_USER));

            mockMvc.perform(delete(REMOVE_ROLE_PATH, SEED_USER_IDS.getFirst())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + userAccessToken)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Remove role while unauthenticated; 401 status code")
        void removeRoleWhileUnauthenticated() throws Exception {
            mockMvc.perform(delete(REMOVE_ROLE_PATH, SEED_USER_IDS.getFirst()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
