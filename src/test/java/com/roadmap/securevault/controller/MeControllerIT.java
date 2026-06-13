package com.roadmap.securevault.controller;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.dto.web.RegisterRequest;
import com.roadmap.securevault.test_util.testcontainers.AbstractSpringBootTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MeController Integration Tests")
@AutoConfigureMockMvc
public class MeControllerIT extends AbstractSpringBootTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private CookieProperties cookieProperties;
    @Autowired private ObjectMapper objectMapper;

    private RegisterRequest uniqueRegisterRequest(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + suffix;
        return new RegisterRequest(username, username + "@example.com", "P4$$word");
    }

    @Test
    @DisplayName("Authenticated; 200 status code")
    void authenticated() throws Exception {
        RegisterRequest register = uniqueRegisterRequest("authenticated");

        MvcResult registerResult = mockMvc.perform(
                        post("/auth/register")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie accessCookie = Arrays.stream(registerResult.getResponse().getCookies())
                .filter(c -> cookieProperties.accessTokenCookieName().equals(c.getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(
                        get("/api/v1/me")
                                .cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(register.username()))
                .andExpect(jsonPath("$.email").value(register.email()))
                .andReturn();
    }

    @Test
    @DisplayName("Unauthenticated; 401 status code")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized());
    }
}
