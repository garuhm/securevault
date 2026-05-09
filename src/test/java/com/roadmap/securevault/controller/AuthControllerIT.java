package com.roadmap.securevault.controller;

import com.roadmap.securevault.dto.RegisterRequest;
import com.roadmap.securevault.entity.Role;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.repo.RoleRepository;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.test_util.testcontainers.AbstractPostgresIT;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController Integration Tests")
@AutoConfigureMockMvc
class AuthControllerIT extends AbstractPostgresIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        roleRepository.save(Role.builder().name(RoleName.ROLE_USER).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_ADMIN).build());
    }

    @AfterEach
    void tearDown() {
        roleRepository.deleteAll();
    }

    @Nested
    @DisplayName("Registration tests")
    class Registration {
        RegisterRequest request = new RegisterRequest("username", "email@email.com", "P4$$word");

        @Test
        @DisplayName("User registration; successful")
        void registerUser() throws Exception {
            mockMvc.perform(
                    post("/auth/register")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            assertThat(userRepository.existsByUsername(request.username())).isTrue();
            assertThat(userRepository.findByUsername(request.username()).get().getRoles()).isNotEmpty();
            assertThat(userRepository.findByUsername(request.username()).get().getRoles())
                    .extracting(Role::getName)
                    .contains(RoleName.ROLE_USER);
        }

        @Test
        @DisplayName("User registration with invalid credentials; 400 status code")
        void registerUserWithInvalidCredentials() throws Exception {
            RegisterRequest badRequest = new RegisterRequest("username", "email", "password");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(badRequest))
                    )
                    .andExpect(status().isBadRequest())
                    .andReturn();

            assertThat(userRepository.existsByUsername(badRequest.username())).isFalse();
            assertThat(userRepository.findByUsername(badRequest.username())).isEqualTo(Optional.empty());
        }

        @Test
        @DisplayName("User registration with duplicate username; 409 status code")
        void registerUserWithDuplicateUsername() throws Exception {
            RegisterRequest badRequest = new RegisterRequest(request.username(), "email2@gmail.com", request.password());

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            mockMvc.perform(
                    post("/auth/register")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(badRequest))
                    )
                    .andExpect(status().isConflict())
                    .andReturn();

            assertThat(userRepository.existsByUsername(badRequest.email())).isFalse();
            assertThat(userRepository.findByUsername(badRequest.email())).isEqualTo(Optional.empty());
        }

        @Test
        @DisplayName("User registration with duplicate email; 409 status code")
        void registerUserWithDuplicateEmail() throws Exception {
            RegisterRequest badRequest = new RegisterRequest("username2", request.email(), request.password());

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(badRequest))
                    )
                    .andExpect(status().isConflict())
                    .andReturn();

            assertThat(userRepository.existsByUsername(badRequest.username())).isFalse();
            assertThat(userRepository.findByUsername(badRequest.username())).isEqualTo(Optional.empty());
        }
    }
}
