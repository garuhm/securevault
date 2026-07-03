package com.roadmap.securevault.controller;

import com.roadmap.securevault.dto.user.RegisterRequest;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.test_util.testcontainers.AbstractSpringBootTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController Integration Tests")
@AutoConfigureMockMvc
class AuthControllerIT extends AbstractSpringBootTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ObjectMapper objectMapper;

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

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Registration tests")
    class Registration {

        @Test
        @DisplayName("User registration; 201 status code, local user synced via Kafka")
        void registerUser() throws Exception {
            RegisterRequest request = uniqueRegisterRequest("reguser");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

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
    @DisplayName("Logout tests")
    class Logout {

        @Test
        @DisplayName("Logout without auth; 401 status code")
        void logoutRequiresAuth() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Logout all sessions with valid token; 200 status code")
        void logoutAllSessions() throws Exception {
            RegisterRequest register = uniqueRegisterRequest("logoutuser");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated());

            String accessToken = obtainAccessToken(register.username(), register.password());

            mockMvc.perform(
                            post("/auth/logout")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isOk());

            mockMvc.perform(
                            post("/auth/logout")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("OAuth2 link tests")
    class OAuth2Link {
        @Test
        @DisplayName("Link without auth; 401 status code")
        void linkRequiresAuth() throws Exception {
            mockMvc.perform(post("/auth/link/google"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Link with invalid provider; 400 status code")
        void linkWithInvalidProvider() throws Exception {
            RegisterRequest register = uniqueRegisterRequest("linkuser");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated());

            String accessToken = obtainAccessToken(register.username(), register.password());
            mockMvc.perform(
                    post("/auth/link/invalidprovider")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Link with valid provider; 302 status code")
        void linkWithValidProvider() throws Exception {
            RegisterRequest register = uniqueRegisterRequest("linkuser");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated());

            String accessToken = obtainAccessToken(register.username(), register.password());
            mockMvc.perform(
                    post("/auth/link/google")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isFound());
        }
    }
}