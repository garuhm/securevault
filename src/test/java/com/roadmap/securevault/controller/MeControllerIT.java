package com.roadmap.securevault.controller;

import com.roadmap.securevault.dto.user.RegisterRequest;
import com.roadmap.securevault.dto.user.RoleUpdateRequest;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.test_util.testcontainers.AbstractSpringBootTest;
import com.roadmap.securevault.util.ApiVersioningResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MeController Integration Tests")
@AutoConfigureMockMvc
public class MeControllerIT extends AbstractSpringBootTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    private static final String ME_PATH = ApiVersioningResolver.resolve(MeController.class, "me", "/me");
    private static final String ROLES_BELOW_PATH = ApiVersioningResolver.resolve(MeController.class, "rolesBelow", "/me/roles-below");

    private static final String ADD_ROLE_PATH = ApiVersioningResolver.resolve(UserController.class, "patchUserRole", "/users/{userId}/roles");

    private RegisterRequest uniqueRegisterRequest(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + suffix;
        return new RegisterRequest(username, username + "@example.com", "P4$$word");
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @DisplayName("Me tests")
    @Nested
    class Me {
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
                            get(ME_PATH)
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(register.username()))
                    .andExpect(jsonPath("$.email").value(register.email()));
        }

        @Test
        @DisplayName("Unauthenticated; 401 status code")
        void unauthenticated() throws Exception {
            mockMvc.perform(get(ME_PATH))
                    .andExpect(status().isUnauthorized());
        }
    }

    @DisplayName("Roles below tests")
    @Nested
    class RolesBelow {
        @Test
        @DisplayName("Authenticated owner returns admin and user roles")
        void rolesBelowOwner() throws Exception {
            RegisterRequest register = uniqueRegisterRequest("authenticatedOwner");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated());

            String accessToken = obtainAccessToken("app_owner", "Owner@1234");
//            String accessToken = obtainAccessToken(register.username(), register.password());

            mockMvc.perform(
                            get(ROLES_BELOW_PATH)
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rolesBelow").isArray())
                    .andExpect(jsonPath("$.rolesBelow.length()").value(2))
                    .andExpect(jsonPath("$.rolesBelow[0]").value("ROLE_ADMIN"))
                    .andExpect(jsonPath("$.rolesBelow[1]").value("ROLE_USER"));

        }

        @Test
        @DisplayName("Authenticated admin returns user roles")
        void rolesBelowAdmin() throws Exception {
            RegisterRequest register = uniqueRegisterRequest("authenticatedAdmin");
            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated());

            String ownerAccessToken = obtainAccessToken("app_owner", "Owner@1234");
            RoleUpdateRequest roleUpdateRequest = new RoleUpdateRequest(Set.of(RoleName.ROLE_ADMIN));
            mockMvc.perform(
                            patch(ADD_ROLE_PATH, userRepository.findByUsername(register.username()).get().getId())
                                    .contentType("application/json")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAccessToken)
                                    .content(objectMapper.writeValueAsString(roleUpdateRequest)))
                    .andExpect(status().isNoContent());

            String adminAccessToken = obtainAccessToken(register.username(), register.password());
            mockMvc.perform(
                    get(ROLES_BELOW_PATH)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rolesBelow").isArray())
                    .andExpect(jsonPath("$.rolesBelow.length()").value(1))
                    .andExpect(jsonPath("$.rolesBelow[0]").value("ROLE_USER"));
        }

        @Test
        @DisplayName("Authenticated user returns empty array")
        void rolesBelowUser() throws Exception {
            RegisterRequest register = uniqueRegisterRequest("authenticatedUser");
            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated());

            mockMvc.perform(
                    get(ROLES_BELOW_PATH)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(register.username(), register.password())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rolesBelow").isArray())
                    .andExpect(jsonPath("$.rolesBelow.length()").value(0));
        }
    }
}