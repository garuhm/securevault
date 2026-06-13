package com.roadmap.securevault.controller;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.dto.web.LoginRequest;
import com.roadmap.securevault.dto.web.RegisterRequest;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.test_util.testcontainers.AbstractSpringBootTest;
import jakarta.servlet.http.Cookie;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController Integration Tests")
@AutoConfigureMockMvc
class AuthControllerIT extends AbstractSpringBootTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CookieProperties cookieProperties;
    @Autowired private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    /**
     * Generates a unique RegisterRequest per call so tests don't collide with
     * leftover Keycloak users from previous test runs/classes sharing the
     * same Testcontainers instance.
     */
    private RegisterRequest uniqueRegisterRequest(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + suffix;
        return new RegisterRequest(username, username + "@example.com", "P4$$word");
    }

    @Nested
    @DisplayName("Registration tests")
    class Registration {

        @Test
        @DisplayName("User registration; 201 status code, cookies set, local user synced via Kafka")
        void registerUser() throws Exception {
            RegisterRequest request = uniqueRegisterRequest("reguser");

            MvcResult result = mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            assertThat(cookieExists(result.getResponse(), cookieProperties.accessTokenCookieName())).isTrue();
            assertThat(cookieExists(result.getResponse(), cookieProperties.refreshTokenCookieName())).isTrue();
            assertThat(extractTokenFromCookie(result.getResponse(), cookieProperties.accessTokenCookieName())).isNotNull();
            assertThat(extractTokenFromCookie(result.getResponse(), cookieProperties.refreshTokenCookieName())).isNotNull();

            // Local User row is populated asynchronously via the Kafka UserCreated event
            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() ->
                            assertThat(userRepository.findAll())
                                    .anyMatch(u -> u.getUsername().equals(request.username())
                                            && u.getEmail().equals(request.email())));
        }

        @Test
        @DisplayName("User registration with invalid credentials; 400 status code")
        void registerUserWithInvalidCredentials() throws Exception {
            RegisterRequest badRequest = new RegisterRequest("baduser-" + UUID.randomUUID(), "not-an-email", "weak");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("User registration with duplicate username; 409 status code")
        void registerUserWithDuplicateUsername() throws Exception {
            RegisterRequest request = uniqueRegisterRequest("dupuser");
            RegisterRequest duplicate = new RegisterRequest(request.username(), "different-" + request.email(), request.password());

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(duplicate)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Login tests")
    class Login {

        @Test
        @DisplayName("User login with valid credentials; 200 status code")
        void login() throws Exception {
            RegisterRequest register = uniqueRegisterRequest("loginuser");
            LoginRequest login = new LoginRequest(register.username(), register.password());

            mockMvc.perform(
                    post("/auth/register")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(register)));

            MvcResult result = mockMvc.perform(
                            post("/auth/login")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(login)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(cookieExists(result.getResponse(), cookieProperties.accessTokenCookieName())).isTrue();
            assertThat(cookieExists(result.getResponse(), cookieProperties.refreshTokenCookieName())).isTrue();
        }

        @Test
        @DisplayName("User login with invalid credentials; 401 status code")
        void loginWithInvalidCredentials() throws Exception {
            RegisterRequest register = uniqueRegisterRequest("loginbaduser");

            mockMvc.perform(
                    post("/auth/register")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(register)));

            mockMvc.perform(
                            post("/auth/login")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(
                                            new LoginRequest(register.username(), "wrongPassword"))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Refresh tests")
    class Refresh {

        @Test
        @DisplayName("Refresh token with valid cookie; 200 status code, new cookies issued")
        void refreshTokenWithValidCookie() throws Exception {
            RegisterRequest register = uniqueRegisterRequest("refreshuser");

            MvcResult registerResult = mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Cookie refreshCookie = Arrays.stream(registerResult.getResponse().getCookies())
                    .filter(c -> cookieProperties.refreshTokenCookieName().equals(c.getName()))
                    .findFirst()
                    .orElseThrow();

            MvcResult refreshResult = mockMvc.perform(
                            post("/auth/refresh")
                                    .cookie(refreshCookie))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(cookieExists(refreshResult.getResponse(), cookieProperties.accessTokenCookieName())).isTrue();
            assertThat(cookieExists(refreshResult.getResponse(), cookieProperties.refreshTokenCookieName())).isTrue();

            // With refresh token rotation enabled, the new refresh token should differ
            String oldRefresh = refreshCookie.getValue();
            String newRefresh = extractTokenFromCookie(refreshResult.getResponse(), cookieProperties.refreshTokenCookieName());
            assertThat(newRefresh).isNotEqualTo(oldRefresh);
        }

        @Test
        @DisplayName("Refresh token with missing cookie; 401 status code")
        void refreshTokenWithNullCookie() throws Exception {
            mockMvc.perform(post("/auth/refresh"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Refresh token with invalid token; 401 status code")
        void refreshTokenWithInvalidToken() throws Exception {
            mockMvc.perform(
                            post("/auth/refresh")
                                    .cookie(new Cookie(cookieProperties.refreshTokenCookieName(), "invalid_token")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Refresh token reuse after rotation; 401 status code")
        void refreshTokenReuseAfterRotation() throws Exception {
            RegisterRequest register = uniqueRegisterRequest("reuseuser");

            MvcResult registerResult = mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Cookie refreshCookie = Arrays.stream(registerResult.getResponse().getCookies())
                    .filter(c -> cookieProperties.refreshTokenCookieName().equals(c.getName()))
                    .findFirst()
                    .orElseThrow();

            // First refresh succeeds, rotating the token
            mockMvc.perform(post("/auth/refresh").cookie(refreshCookie))
                    .andExpect(status().isOk());

            // Reusing the now-rotated-out token should fail
            mockMvc.perform(post("/auth/refresh").cookie(refreshCookie))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Logout tests")
    class Logout {

        @Test
        @DisplayName("Logout; 200 status code, refresh token revoked")
        void logout() throws Exception {
            RegisterRequest register = uniqueRegisterRequest("logoutuser");

            MvcResult registerResult = mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Cookie refreshCookie = Arrays.stream(registerResult.getResponse().getCookies())
                    .filter(c -> cookieProperties.refreshTokenCookieName().equals(c.getName()))
                    .findFirst()
                    .orElseThrow();

            mockMvc.perform(post("/auth/logout").cookie(refreshCookie))
                    .andExpect(status().isOk());

            // Revoked refresh token should now fail
            mockMvc.perform(post("/auth/refresh").cookie(refreshCookie))
                    .andExpect(status().isUnauthorized());
        }
    }

    private String extractTokenFromCookie(MockHttpServletResponse response, String cookieName) {
        return Arrays.stream(response.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean cookieExists(MockHttpServletResponse response, String cookieName) {
        return Arrays.stream(response.getCookies())
                .anyMatch(c -> cookieName.equals(c.getName()));
    }
}