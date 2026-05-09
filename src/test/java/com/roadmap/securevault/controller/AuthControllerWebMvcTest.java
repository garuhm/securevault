package com.roadmap.securevault.controller;

import com.roadmap.securevault.config.SecurityConfig;
import com.roadmap.securevault.dto.RegisterRequest;
import com.roadmap.securevault.exception.CredentialsTakenException;
import com.roadmap.securevault.exception.GlobalExceptionHandler;
import com.roadmap.securevault.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AuthController WebMvc Tests")
class AuthControllerWebMvcTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("/auth/register")
    class Register {
        @Test
        @DisplayName("Register new user with valid credentials; successful")
        void registerNewUserWithValidCredentials() throws Exception {
            doNothing().when(userService).register(any(RegisterRequest.class));

            RegisterRequest request = new RegisterRequest("username", "email@email.com", "P4$$word");

            mockMvc.perform(
                    post("/auth/register")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Register new user with invalid credentials; 400 status code")
        void registerNewUserWithInvalidCredentials() throws Exception {
            doNothing().when(userService).register(any(RegisterRequest.class));

            RegisterRequest request = new RegisterRequest("username", "email.com", "password");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Register new user with duplicate username; 409 status code")
        void registerNewUserWithDuplicateUsername() throws Exception {
            doThrow(new CredentialsTakenException("Username already exists")).when(userService).register(any(RegisterRequest.class));

            RegisterRequest request = new RegisterRequest("username", "email@email.com", "P4$$word");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Register new user with duplicate email; 409 status code")
        void registerNewUserWithDuplicateEmail() throws Exception {
            doThrow(new CredentialsTakenException("Username already exists")).when(userService).register(any(RegisterRequest.class));

            RegisterRequest request = new RegisterRequest("username", "email@email.com", "P4$$word");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isConflict());
        }
    }
}
