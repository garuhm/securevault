package com.roadmap.securevault.controller;

import com.roadmap.securevault.config.SecurityConfig;
import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.JwtProperties;
import com.roadmap.securevault.dto.LoginRequest;
import com.roadmap.securevault.dto.RegisterRequest;
import com.roadmap.securevault.exception.CredentialsTakenException;
import com.roadmap.securevault.exception.GlobalExceptionHandler;
import com.roadmap.securevault.exception.InvalidRefreshTokenException;
import com.roadmap.securevault.service.AccessJwtService;
import com.roadmap.securevault.service.AuthService;
import com.roadmap.securevault.service.CookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
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
    private CookieService cookieService;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private AccessJwtService accessJwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private JwtProperties jwtPropertiesBean;
    @MockitoBean
    private CookieProperties cookiePropertiesBean;
    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("/auth/register")
    class Register {
        @Test
        @DisplayName("Register new user with valid credentials; successful")
        void registerNewUserWithValidCredentials() throws Exception {
            doNothing().when(authService).register(any(RegisterRequest.class), any(HttpServletResponse.class));

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
            doNothing().when(authService).register(any(RegisterRequest.class), any(HttpServletResponse.class));

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
            doThrow(new CredentialsTakenException("Username already exists")).when(authService).register(any(RegisterRequest.class), any(HttpServletResponse.class));

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
            doThrow(new CredentialsTakenException("Username already exists")).when(authService).register(any(RegisterRequest.class), any(HttpServletResponse.class));

            RegisterRequest request = new RegisterRequest("username", "email@email.com", "P4$$word");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("/auth/login")
    class Login {
        @Test
        @DisplayName("Login with valid credentials; successful")
        void loginWithValidCredentials() throws Exception {
            doNothing().when(authService).login(any(LoginRequest.class), any(HttpServletResponse.class));

            LoginRequest request = new LoginRequest("username", "P4$$word");

            mockMvc.perform(
                            post("/auth/login")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Login with invalid credentials; exception thrown")
        void loginWithInvalidCredentials() throws Exception {
            doThrow(BadCredentialsException.class).when(authService).login(any(LoginRequest.class), any(HttpServletResponse.class));

            LoginRequest request = new LoginRequest("username", "invalid_password");

            mockMvc.perform(
                            post("/auth/login")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("/auth/refresh")
    class Refresh {
        @Test
        @DisplayName("Validate refresh token; successful")
        void validateRefreshToken() throws Exception {
            doNothing().when(authService).refreshToken(any(HttpServletRequest.class) ,any(HttpServletResponse.class));

            mockMvc.perform(
                            post("/auth/refresh")
                                    .contentType("application/json")
                    )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Validate refresh token with invalid cookie; exception thrown")
        void validateRefreshTokenWithInvalidCookie() throws Exception {
            doThrow(InvalidCookieException.class).when(authService).refreshToken(any(HttpServletRequest.class) ,any(HttpServletResponse.class));
            mockMvc.perform(
                            post("/auth/refresh")
                                    .contentType("application/json")
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Validate refresh token with invalid refresh token; exception thrown")
        void validateRefreshTokenWithInvalidRefreshToken() throws Exception {
            doThrow(InvalidRefreshTokenException.class).when(authService).refreshToken(any(HttpServletRequest.class) ,any(HttpServletResponse.class));
            mockMvc.perform(
                            post("/auth/refresh")
                                    .contentType("application/json")
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("/auth/logout")
    class Logout {
        @Test
        @DisplayName("Logout; successful")
        void logout() throws Exception {
            doNothing().when(authService).logout(any(HttpServletRequest.class) ,any(HttpServletResponse.class));

            mockMvc.perform(
                            post("/auth/logout")
                                    .contentType("application/json")
                    )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Logout all sessions (/auth/logout?all=true); successful")
        void logoutAllSessions() throws Exception {
            doNothing().when(authService).logoutAllSessions(any(HttpServletResponse.class));

            mockMvc.perform(
                            post("/auth/logout?all=true")
                                    .contentType("application/json")
                    )
                    .andExpect(status().isOk());
        }
    }
}
