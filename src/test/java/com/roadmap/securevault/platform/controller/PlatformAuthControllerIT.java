package com.roadmap.securevault.platform.controller;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.platform.dto.PlatformUserRegisterRequest;
import com.roadmap.securevault.platform.entity.PlatformUser;
import com.roadmap.securevault.platform.entity.enums.PlatformRole;
import com.roadmap.securevault.platform.repo.PlatformRefreshTokenRepository;
import com.roadmap.securevault.platform.repo.PlatformUserRepository;
import com.roadmap.securevault.test_util.testcontainers.AbstractIT;
import com.roadmap.securevault.test_util.web.ApiVersioningResolver;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PlatformAuthController Integration Tests")
class PlatformAuthControllerIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired PlatformUserRepository userRepository;
    @Autowired PlatformRefreshTokenRepository refreshTokenRepository;
    @Autowired CookieProperties cookieProperties;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    // seeded owner credentials from V3 Flyway migration
    private static final String OWNER_USERNAME = "owner";
    private static final String OWNER_PASSWORD = "Owner@1234";

    private static final String LOGIN_URL =
            ApiVersioningResolver.resolve(PlatformAuthController.class, "login", "/platform/auth/login");
    private static final String REFRESH_URL =
            ApiVersioningResolver.resolve(PlatformAuthController.class, "refreshToken", "/platform/auth/refresh");
    private static final String LOGOUT_URL =
            ApiVersioningResolver.resolve(PlatformAuthController.class, "logout", "/platform/auth/logout");
    private static final String REGISTER_URL =
            ApiVersioningResolver.resolve(PlatformAuthController.class, "register", "/platform/auth/register");

    @BeforeEach
    void setUp() {
        seedOwner();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        // re-seed owner so subsequent tests work
    }

    void seedOwner() {
        if (!userRepository.existsByUsername(OWNER_USERNAME)) {
            PlatformUser owner = PlatformUser.builder()
                    .username(OWNER_USERNAME)
                    .email("owner@securevault.com")
                    .password(passwordEncoder.encode(OWNER_PASSWORD))
                    .role(PlatformRole.PLATFORM_OWNER)
                    .build();
            userRepository.saveAndFlush(owner);
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

    String extractCookieValue(MvcResult result, String cookieName) {
        return Arrays.stream(result.getResponse().getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    boolean cookieExists(MvcResult result, String cookieName) {
        return Arrays.stream(result.getResponse().getCookies())
                .anyMatch(c -> cookieName.equals(c.getName()));
    }

    @Nested
    @DisplayName("Login")
    class Login {

        @Test
        @DisplayName("Valid credentials → 200 + cookies set")
        void loginSuccess() throws Exception {
            MvcResult result = mockMvc.perform(post(LOGIN_URL)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(OWNER_USERNAME, OWNER_PASSWORD))))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(cookieExists(result, cookieProperties.accessTokenCookieName())).isTrue();
            assertThat(cookieExists(result, cookieProperties.refreshTokenCookieName())).isTrue();
            assertThat(extractCookieValue(result, cookieProperties.accessTokenCookieName())).isNotBlank();
            assertThat(extractCookieValue(result, cookieProperties.refreshTokenCookieName())).isNotBlank();
            assertThat(refreshTokenRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("Wrong password → 401")
        void loginWrongPassword() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(OWNER_USERNAME, "WrongPass@1"))))
                    .andExpect(status().isUnauthorized());

            assertThat(refreshTokenRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Unknown username → 401")
        void loginUnknownUser() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest("nobody", OWNER_PASSWORD))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Register")
    class Register {

        @Test
        @DisplayName("Owner creates admin → 201")
        void registerAdmin() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            PlatformUserRegisterRequest request =
                    new PlatformUserRegisterRequest("newadmin", "admin@test.com", "Admin@1234");

            mockMvc.perform(post(REGISTER_URL)
                            .queryParam("role", "PLATFORM_ADMIN")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request))
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName())))
                    .andExpect(status().isCreated());

            assertThat(userRepository.existsByUsername("newadmin")).isTrue();
            assertThat(userRepository.findByUsername("newadmin").get().getRole())
                    .isEqualTo(PlatformRole.PLATFORM_ADMIN);
        }

        @Test
        @DisplayName("Unauthenticated register → 401")
        void registerUnauthenticated() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .queryParam("role", "PLATFORM_ADMIN")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new PlatformUserRegisterRequest("newadmin", "admin@test.com", "Admin@1234"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Duplicate username → 409")
        void registerDuplicateUsername() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
            String accessToken = extractCookieValue(loginResult, cookieProperties.accessTokenCookieName());

            PlatformUserRegisterRequest request =
                    new PlatformUserRegisterRequest(OWNER_USERNAME, "other@test.com", "Admin@1234");

            mockMvc.perform(post(REGISTER_URL)
                            .queryParam("role", "PLATFORM_ADMIN")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request))
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName())))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Refresh")
    class Refresh {

        @Test
        @DisplayName("Valid refresh token → 200 + new cookies")
        void refreshSuccess() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult refreshResult = mockMvc.perform(post(REFRESH_URL)
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName()))
                            .cookie(extractCookie(loginResult, cookieProperties.refreshTokenCookieName())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(cookieExists(refreshResult, cookieProperties.accessTokenCookieName())).isTrue();
            assertThat(cookieExists(refreshResult, cookieProperties.refreshTokenCookieName())).isTrue();
            assertThat(refreshTokenRepository.findAll()).hasSize(2);
            assertThat(refreshTokenRepository
                    .findAllByUserAndRevokedFalse(userRepository.findByUsername(OWNER_USERNAME).get()))
                    .hasSize(1);
        }

        @Test
        @DisplayName("Missing refresh cookie → 401")
        void refreshNoCookie() throws Exception {
            mockMvc.perform(post(REFRESH_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Invalid refresh token → 401")
        void refreshInvalidToken() throws Exception {
            mockMvc.perform(post(REFRESH_URL)
                            .cookie(new Cookie(cookieProperties.refreshTokenCookieName(), "invalid")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Revoked refresh token → 401")
        void refreshRevokedToken() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
            String refreshTokenValue = extractCookieValue(loginResult, cookieProperties.refreshTokenCookieName());

            refreshTokenRepository.findById(UUID.fromString(refreshTokenValue))
                    .ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepository.saveAndFlush(rt);
                    });

            mockMvc.perform(post(REFRESH_URL)
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName()))
                            .cookie(extractCookie(loginResult, cookieProperties.refreshTokenCookieName())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Expired refresh token → 401")
        void refreshExpiredToken() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
            String refreshTokenValue = extractCookieValue(loginResult, cookieProperties.refreshTokenCookieName());

            refreshTokenRepository.findById(UUID.fromString(refreshTokenValue))
                    .ifPresent(rt -> {
                        rt.setExpiryDate(new Date(System.currentTimeMillis() - 1000));
                        refreshTokenRepository.saveAndFlush(rt);
                    });

            mockMvc.perform(post(REFRESH_URL)
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName()))
                            .cookie(extractCookie(loginResult, cookieProperties.refreshTokenCookieName())))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Logout")
    class Logout {

        @Test
        @DisplayName("Valid logout → 200 + cookies cleared")
        void logoutSuccess() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult logoutResult = mockMvc.perform(post(LOGOUT_URL)
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName()))
                            .cookie(extractCookie(loginResult, cookieProperties.refreshTokenCookieName())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(refreshTokenRepository
                    .findAllByUserAndRevokedFalse(
                            userRepository.findByUsername(OWNER_USERNAME).get()))
                    .isEmpty();
            Cookie clearedAccess = Arrays.stream(logoutResult.getResponse().getCookies())
                    .filter(c -> cookieProperties.accessTokenCookieName().equals(c.getName()))
                    .findFirst().orElse(null);
            assertThat(clearedAccess).isNotNull();
            assertThat(clearedAccess.getMaxAge()).isZero();
        }

        @Test
        @DisplayName("Logout all sessions → 200 + all tokens revoked")
        void logoutAllSessions() throws Exception {
            MvcResult login1 = login(OWNER_USERNAME, OWNER_PASSWORD);
            MvcResult login2 = login(OWNER_USERNAME, OWNER_PASSWORD);

            mockMvc.perform(post(LOGOUT_URL)
                            .queryParam("all", "true")
                            .cookie(extractCookie(login2, cookieProperties.accessTokenCookieName()))
                            .cookie(extractCookie(login2, cookieProperties.refreshTokenCookieName())))
                    .andExpect(status().isOk());

            assertThat(refreshTokenRepository
                    .findAllByUserAndRevokedFalse(
                            userRepository.findByUsername(OWNER_USERNAME).get()))
                    .isEmpty();
        }
    }
}