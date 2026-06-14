package com.roadmap.securevault.controller;

import com.roadmap.securevault.dto.web.RegisterRequest;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.test_util.testcontainers.AbstractSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MeController Integration Tests")
@AutoConfigureMockMvc
public class MeControllerIT extends AbstractSpringBootTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    private RegisterRequest uniqueRegisterRequest(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + suffix;
        return new RegisterRequest(username, username + "@example.com", "P4$$word");
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Authenticated; 200 status code")
    void authenticated() throws Exception {
        RegisterRequest register = uniqueRegisterRequest("authenticated");

        mockMvc.perform(
                        post("/auth/register")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        String accessToken = obtainAccessToken(register.username(), register.password());

        mockMvc.perform(
                        get("/api/v1/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(register.username()))
                .andExpect(jsonPath("$.email").value(register.email()));
    }

    @Test
    @DisplayName("Unauthenticated; 401 status code")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }
}