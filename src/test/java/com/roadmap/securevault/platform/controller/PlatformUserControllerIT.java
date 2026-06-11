package com.roadmap.securevault.platform.controller;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.platform.dto.PlatformUserUpdateRequest;
import com.roadmap.securevault.platform.entity.PlatformUser;
import com.roadmap.securevault.platform.entity.enums.PlatformRole;
import com.roadmap.securevault.platform.repo.PlatformRefreshTokenRepository;
import com.roadmap.securevault.platform.repo.PlatformUserRepository;
import com.roadmap.securevault.test_util.testcontainers.AbstractIT;
import com.roadmap.securevault.common.util.ApiVersioningResolver;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PlatformUserController Integration Tests")
class PlatformUserControllerIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired PlatformUserRepository userRepository;
    @Autowired PlatformRefreshTokenRepository refreshTokenRepository;
    @Autowired CookieProperties cookieProperties;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String OWNER_USERNAME = "owner";
    private static final String OWNER_PASSWORD = "Owner@1234";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@1234";
    private static final String SUPPORT_USERNAME = "support";
    private static final String SUPPORT_PASSWORD = "Support@1234";

    private static final String LOGIN_URL =
            ApiVersioningResolver.resolve(PlatformAuthController.class, "login", "/platform/auth/login");
    private static final String USERS_URL =
            ApiVersioningResolver.resolve(PlatformUserController.class, "getUsers", "/platform/users");
    private static final String USER_BY_ID_URL =
            ApiVersioningResolver.resolve(PlatformUserController.class, "getUserById", "/platform/users/{id}");
    private static final String UPDATE_USER_URL =
            ApiVersioningResolver.resolve(PlatformUserController.class, "updateUser", "/platform/users/{id}");
    private static final String DELETE_USER_URL =
            ApiVersioningResolver.resolve(PlatformUserController.class, "deleteUser", "/platform/users/{id}");

    @BeforeEach
    void setUp() {
        seedUsers();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    void seedUsers() {
        if (!userRepository.existsByUsername(OWNER_USERNAME)) {
            userRepository.save(PlatformUser.builder()
                    .username(OWNER_USERNAME)
                    .email("owner@securevault.com")
                    .password(passwordEncoder.encode(OWNER_PASSWORD))
                    .role(PlatformRole.PLATFORM_OWNER)
                    .build());
        }
        if (!userRepository.existsByUsername(ADMIN_USERNAME)) {
            userRepository.save(PlatformUser.builder()
                    .username(ADMIN_USERNAME)
                    .email("admin@securevault.com")
                    .password(passwordEncoder.encode(ADMIN_PASSWORD))
                    .role(PlatformRole.PLATFORM_ADMIN)
                    .build());
        }
        if (!userRepository.existsByUsername(SUPPORT_USERNAME)) {
            userRepository.save(PlatformUser.builder()
                    .username(SUPPORT_USERNAME)
                    .email("support@securevault.com")
                    .password(passwordEncoder.encode(SUPPORT_PASSWORD))
                    .role(PlatformRole.PLATFORM_SUPPORT)
                    .build());
        }
    }

    MvcResult login(String username, String password) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andReturn();
    }

    Cookie extractCookie(MvcResult result, String cookieName) {
        return Arrays.stream(result.getResponse().getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .findFirst()
                .orElse(null);
    }

    MvcResult authenticatedGet(String url, MvcResult result, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)
                        .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                .andReturn();
    }

    MvcResult authenticatedPut(String url, MvcResult result, Object body, Object... uriVars) throws Exception {
        return mockMvc.perform(put(url, uriVars)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body))
                        .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                .andReturn();
    }

    MvcResult authenticatedDelete(String url, MvcResult result, Object... uriVars) throws Exception {
        return mockMvc.perform(delete(url, uriVars)
                        .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                .andReturn();
    }

    @Nested
    @DisplayName("GET /platform/users")
    class GetUsers {

        @Test
        @DisplayName("Owner → 200 + page")
        void ownerCanGetUsers() throws Exception {
            MvcResult result = login(OWNER_USERNAME, OWNER_PASSWORD);

            mockMvc.perform(get(USERS_URL)
                            .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin → 200 + page")
        void adminCanGetUsers() throws Exception {
            MvcResult result = login(ADMIN_USERNAME, ADMIN_PASSWORD);

            mockMvc.perform(get(USERS_URL)
                            .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Support → 200 + page")
        void supportCanGetUsers() throws Exception {
            MvcResult result = login(SUPPORT_USERNAME, SUPPORT_PASSWORD);

            mockMvc.perform(get(USERS_URL)
                            .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedCannotGetUsers() throws Exception {
            mockMvc.perform(get(USERS_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Filter by role → 200 filtered results")
        void filterByRole() throws Exception {
            MvcResult result = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result2 = mockMvc.perform(get(USERS_URL)
                            .queryParam("role", "PLATFORM_ADMIN")
                            .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                    .andExpect(status().isOk())
                    .andReturn();

            String body = result2.getResponse().getContentAsString();
            assertThat(body).contains("PLATFORM_ADMIN");
            assertThat(body).doesNotContain("PLATFORM_OWNER");
            assertThat(body).doesNotContain("PLATFORM_SUPPORT");
        }
    }

    @Nested
    @DisplayName("GET /platform/users/{id}")
    class GetUserById {

        @Test
        @DisplayName("Valid id → 200")
        void validIdReturnsUser() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
            UUID adminId = userRepository.findByUsername(ADMIN_USERNAME).get().getId();

            MvcResult result = authenticatedGet(USER_BY_ID_URL, loginResult, adminId);

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(result.getResponse().getContentAsString()).contains(ADMIN_USERNAME);
        }

        @Test
        @DisplayName("Non-existent id → 404")
        void nonExistentIdReturns404() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            authenticatedGet(USER_BY_ID_URL, loginResult, UUID.randomUUID());

            MvcResult result = authenticatedGet(USER_BY_ID_URL, loginResult, UUID.randomUUID());
            assertThat(result.getResponse().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedReturns401() throws Exception {
            UUID adminId = userRepository.findByUsername(ADMIN_USERNAME).get().getId();

            mockMvc.perform(get(USER_BY_ID_URL, adminId))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /platform/users/{id}")
    class UpdateUser {

        @Test
        @DisplayName("Self update → 200")
        void selfUpdate() throws Exception {
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);
            UUID adminId = userRepository.findByUsername(ADMIN_USERNAME).get().getId();

            PlatformUserUpdateRequest request = new PlatformUserUpdateRequest(
                    "newadminname", null, null);

            MvcResult result = authenticatedPut(UPDATE_USER_URL, loginResult, request, adminId);

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(userRepository.existsByUsername("newadminname")).isTrue();
        }

        @Test
        @DisplayName("Owner updates admin → 200")
        void ownerUpdatesAdmin() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
            UUID adminId = userRepository.findByUsername(ADMIN_USERNAME).get().getId();

            PlatformUserUpdateRequest request = new PlatformUserUpdateRequest(
                    "updatedadmin", null, null);

            MvcResult result = authenticatedPut(UPDATE_USER_URL, loginResult, request, adminId);

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(userRepository.existsByUsername("updatedadmin")).isTrue();
        }

        @Test
        @DisplayName("Admin updates owner → 403")
        void adminCannotUpdateOwner() throws Exception {
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);
            UUID ownerId = userRepository.findByUsername(OWNER_USERNAME).get().getId();

            PlatformUserUpdateRequest request = new PlatformUserUpdateRequest(
                    "hackedowner", null, null);

            MvcResult result = authenticatedPut(UPDATE_USER_URL, loginResult, request, ownerId);

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(userRepository.existsByUsername("hackedowner")).isFalse();
        }

        @Test
        @DisplayName("Support updates admin → 403")
        void supportCannotUpdateAdmin() throws Exception {
            MvcResult loginResult = login(SUPPORT_USERNAME, SUPPORT_PASSWORD);
            UUID adminId = userRepository.findByUsername(ADMIN_USERNAME).get().getId();

            PlatformUserUpdateRequest request = new PlatformUserUpdateRequest(
                    "hackedadmin", null, null);

            MvcResult result = authenticatedPut(UPDATE_USER_URL, loginResult, request, adminId);

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("Duplicate username → 409")
        void duplicateUsernameReturns409() throws Exception {
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);
            UUID adminId = userRepository.findByUsername(ADMIN_USERNAME).get().getId();

            PlatformUserUpdateRequest request = new PlatformUserUpdateRequest(
                    OWNER_USERNAME, null, null);

            MvcResult result = authenticatedPut(UPDATE_USER_URL, loginResult, request, adminId);

            assertThat(result.getResponse().getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedReturns401() throws Exception {
            UUID adminId = userRepository.findByUsername(ADMIN_USERNAME).get().getId();

            mockMvc.perform(put(UPDATE_USER_URL, adminId)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new PlatformUserUpdateRequest("newname", null, null))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /platform/users/{id}")
    class DeleteUser {

        @Test
        @DisplayName("Owner deletes admin → 204")
        void ownerDeletesAdmin() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
            UUID adminId = userRepository.findByUsername(ADMIN_USERNAME).get().getId();

            MvcResult result = authenticatedDelete(DELETE_USER_URL, loginResult, adminId);

            assertThat(result.getResponse().getStatus()).isEqualTo(204);
            assertThat(userRepository.existsByUsername(ADMIN_USERNAME)).isFalse();
        }

        @Test
        @DisplayName("Admin deletes support → 204")
        void adminDeletesSupport() throws Exception {
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);
            UUID supportId = userRepository.findByUsername(SUPPORT_USERNAME).get().getId();

            MvcResult result = authenticatedDelete(DELETE_USER_URL, loginResult, supportId);

            assertThat(result.getResponse().getStatus()).isEqualTo(204);
            assertThat(userRepository.existsByUsername(SUPPORT_USERNAME)).isFalse();
        }

        @Test
        @DisplayName("Delete owner → 403")
        void cannotDeleteOwner() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
            UUID ownerId = userRepository.findByUsername(OWNER_USERNAME).get().getId();

            MvcResult result = authenticatedDelete(DELETE_USER_URL, loginResult, ownerId);

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(userRepository.existsByUsername(OWNER_USERNAME)).isTrue();
        }

        @Test
        @DisplayName("Admin deletes owner → 403")
        void adminCannotDeleteOwner() throws Exception {
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);
            UUID ownerId = userRepository.findByUsername(OWNER_USERNAME).get().getId();

            MvcResult result = authenticatedDelete(DELETE_USER_URL, loginResult, ownerId);

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(userRepository.existsByUsername(OWNER_USERNAME)).isTrue();
        }

        @Test
        @DisplayName("Same rank delete → 403")
        void sameRankCannotDelete() throws Exception {
            userRepository.save(PlatformUser.builder()
                    .username("admin2")
                    .email("admin2@securevault.com")
                    .password(passwordEncoder.encode("Admin2@1234"))
                    .role(PlatformRole.PLATFORM_ADMIN)
                    .build());

            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);
            UUID admin2Id = userRepository.findByUsername("admin2").get().getId();

            MvcResult result = authenticatedDelete(DELETE_USER_URL, loginResult, admin2Id);

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(userRepository.existsByUsername("admin2")).isTrue();
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedReturns401() throws Exception {
            UUID adminId = userRepository.findByUsername(ADMIN_USERNAME).get().getId();

            mockMvc.perform(delete(DELETE_USER_URL, adminId))
                    .andExpect(status().isUnauthorized());
        }
    }
}