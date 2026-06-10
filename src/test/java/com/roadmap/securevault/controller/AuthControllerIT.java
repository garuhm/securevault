package com.roadmap.securevault.controller;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.config.properties.JwtProperties;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.platform.dto.PlatformRegisterRequest;
import com.roadmap.securevault.entity.Role;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.common.repo.BaseRefreshTokenRepository;
import com.roadmap.securevault.common.repo.BaseUserRepository;
import com.roadmap.securevault.common.service.CookieService;
import com.roadmap.securevault.test_util.testcontainers.AbstractPostgresIT;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController Integration Tests")
@AutoConfigureMockMvc
class AuthControllerIT extends AbstractPostgresIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BaseUserRepository baseUserRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private BaseRefreshTokenRepository baseRefreshTokenRepository;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private CookieProperties cookieProperties;
    @Autowired
    private CookieService cookieService;
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
        baseRefreshTokenRepository.deleteAll();
        baseUserRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Nested
    @DisplayName("Registration tests")
    class Registration {

        PlatformRegisterRequest request = new PlatformRegisterRequest("username", "email@email.com", "P4$$word");

        @Test
        @Transactional
        @DisplayName("User registration; 201 status code")
        void registerUser() throws Exception {
            MvcResult result = mockMvc.perform(
                    post("/auth/register")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            assertThat(baseUserRepository.existsByUsername(request.username())).isTrue();
            assertThat(baseUserRepository.findByUsername(request.username()).get().getRoles()).isNotEmpty();
            assertThat(baseUserRepository.findByUsername(request.username()).get().getRoles())
                    .extracting(Role::getName)
                    .contains(RoleName.ROLE_USER);
            assertThat(baseRefreshTokenRepository.findAllByUserAndRevokedFalse(baseUserRepository.findByUsername(request.username()).get()).size()).isEqualTo(1);
            assertThat(baseRefreshTokenRepository.findAll().size()).isEqualTo(1);
            assertThat(baseRefreshTokenRepository
                    .findById(
                            UUID.fromString(
                                    extractTokenFromCookie(
                                            result.getResponse(),
                                            cookieProperties.refreshTokenCookieName())))
                    .isPresent())
                    .isTrue();
            assertThat(cookieExists(result.getResponse(), cookieProperties.accessTokenCookieName())).isTrue();
            assertThat(cookieExists(result.getResponse(), cookieProperties.refreshTokenCookieName())).isTrue();
            assertThat(extractTokenFromCookie(result.getResponse(), cookieProperties.accessTokenCookieName())).isNotNull();
            assertThat(extractTokenFromCookie(result.getResponse(), cookieProperties.refreshTokenCookieName())).isNotNull();
        }

        @Test
        @DisplayName("User registration with invalid credentials; 400 status code")
        void registerUserWithInvalidCredentials() throws Exception {
            PlatformRegisterRequest badRequest = new PlatformRegisterRequest("username", "email", "password");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(badRequest))
                    )
                    .andExpect(status().isBadRequest())
                    .andReturn();

            assertThat(baseUserRepository.existsByUsername(badRequest.username())).isFalse();
            assertThat(baseUserRepository.findByUsername(badRequest.username())).isEqualTo(Optional.empty());
            assertThat(baseRefreshTokenRepository.findAll().size()).isEqualTo(0);
        }

        @Test
        @DisplayName("User registration with duplicate username; 409 status code")
        @Transactional
        void registerUserWithDuplicateUsername() throws Exception {
            PlatformRegisterRequest badRequest = new PlatformRegisterRequest(request.username(), "email2@gmail.com", request.password());

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

            assertThat(baseUserRepository.existsByUsername(badRequest.email())).isFalse();
            assertThat(baseUserRepository.findByUsername(badRequest.email())).isEqualTo(Optional.empty());
            assertThat(baseRefreshTokenRepository.findAll().size()).isEqualTo(1);
        }

        @Test
        @DisplayName("User registration with duplicate email; 409 status code")
        @Transactional
        void registerUserWithDuplicateEmail() throws Exception {
            PlatformRegisterRequest badRequest = new PlatformRegisterRequest("username2", request.email(), request.password());

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

            assertThat(baseUserRepository.existsByUsername(badRequest.username())).isFalse();
            assertThat(baseUserRepository.findByUsername(badRequest.username())).isEqualTo(Optional.empty());
            assertThat(baseRefreshTokenRepository.findAll().size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Login tests")
    class Login {
        PlatformRegisterRequest register = new PlatformRegisterRequest("username", "email@email.com", "P4$$word");
        LoginRequest login = new LoginRequest(register.username(), register.password());

        @Test
        @DisplayName("User login with valid credentials; 200 status code")
        @Transactional
        void login() throws Exception {
            mockMvc.perform(
                    post("/auth/register")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(register))
            );
            MvcResult result = mockMvc.perform(
                    post("/auth/login")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();

            User user = baseUserRepository.findByUsername(register.username()).get();

            assertThat(baseRefreshTokenRepository.findAll().size()).isEqualTo(2);
            assertThat(baseRefreshTokenRepository.findAllByUserAndRevokedFalse(user).size()).isEqualTo(2);
            assertThat(cookieExists(result.getResponse(), cookieProperties.accessTokenCookieName())).isTrue();
            assertThat(cookieExists(result.getResponse(), cookieProperties.refreshTokenCookieName())).isTrue();
            assertThat(extractTokenFromCookie(result.getResponse(), cookieProperties.accessTokenCookieName())).isNotNull();
            assertThat(extractTokenFromCookie(result.getResponse(), cookieProperties.refreshTokenCookieName())).isNotNull();
        }

        @Test
        @DisplayName("User login with invalid credentials; 401 status code")
        @Transactional
        void loginWithInvalidCredentials() throws Exception {
            LoginRequest badUserReq = new LoginRequest("bad username", register.password());
            LoginRequest badPasswordReq = new LoginRequest(register.username(), "badPassword");
            mockMvc.perform(
                    post("/auth/register")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(register))
            );
            mockMvc.perform(
                    post("/auth/login")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(badUserReq)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
            mockMvc.perform(
                    post("/auth/login")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(badPasswordReq))
                    )
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            User user = baseUserRepository.findByUsername(register.username()).get();

            assertThat(baseRefreshTokenRepository.findAll().size()).isEqualTo(1);
            assertThat(baseRefreshTokenRepository.findAllByUserAndRevokedFalse(user).size()).isEqualTo(1);
        }

    }

    @Nested
    @DisplayName("Refresh tests")
    class Refresh {
        PlatformRegisterRequest register = new PlatformRegisterRequest("username", "email@email.com", "P4$$word");

        // success
        @Test
        @DisplayName("Refresh token with valid cookies; 200 status code")
        @Transactional
        void refreshTokenWithValidCookies() throws Exception {
            MvcResult registerResult = mockMvc.perform(
                    post("/auth/register")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(register)))
                    .andReturn();

            Cookie refreshTokenCookie = Arrays.stream(registerResult.getResponse().getCookies())
                    .filter(cookie -> cookieProperties.refreshTokenCookieName().equals(cookie.getName()))
                    .findFirst()
                    .orElse(null);

            MvcResult refreshResult = mockMvc.perform(
                    post("/auth/refresh")
                            .cookie(new Cookie(cookieProperties.refreshTokenCookieName(), refreshTokenCookie.getValue())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(baseRefreshTokenRepository.findAll().size()).isEqualTo(2);
            assertThat(baseRefreshTokenRepository
                    .findAllByUserAndRevokedFalse(
                            baseUserRepository
                                    .findByUsername(register.username())
                                    .get())
                    .size())
                    .isEqualTo(1);
            assertThat(cookieExists(
                    refreshResult.getResponse(),
                    cookieProperties.accessTokenCookieName()))
                    .isTrue();
            assertThat(cookieExists(
                    refreshResult.getResponse(),
                    cookieProperties.refreshTokenCookieName()))
                    .isTrue();
            assertThat(extractTokenFromCookie(refreshResult.getResponse(), cookieProperties.accessTokenCookieName())).isNotNull();
            assertThat(extractTokenFromCookie(refreshResult.getResponse(), cookieProperties.refreshTokenCookieName())).isNotNull();
        }
        // null cookie
        @Test
        @DisplayName("Refresh token with null cookie; 401 status code")
        @Transactional
        void refreshTokenWithNullCookie() throws Exception {
            mockMvc.perform(
                    post("/auth/refresh"))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
        }
        // invalid token
        @Test
        @DisplayName("Refresh token with invalid token; 401 status code")
        @Transactional
        void refreshTokenWithInvalidToken() throws Exception {
            mockMvc.perform(
                    post("/auth/refresh")
                            .cookie(new Cookie(cookieProperties.refreshTokenCookieName(), "invalid_token")))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
        }
        // non-existent token
        @Test
        @DisplayName("Refresh token with non-existent token; 401 status code")
        @Transactional
        void refreshTokenWithNonExistentToken() throws Exception {
            mockMvc.perform(
                    post("/auth/refresh")
                            .cookie(new Cookie(cookieProperties.refreshTokenCookieName(), UUID.randomUUID().toString())))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
        }
        // revoked token
        @Test
        @DisplayName("Refresh token with revoked token; 401 status code")
        @Transactional
        void refreshTokenWithRevokedToken() throws Exception {
            MvcResult registerResult = mockMvc.perform(
                            post("/auth/register")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(register)))
                    .andReturn();

            String refreshToken = extractTokenFromCookie(registerResult.getResponse(), cookieProperties.refreshTokenCookieName());
            baseRefreshTokenRepository.findById(UUID.fromString(refreshToken)).ifPresent(rt ->
                    {
                        rt.setRevoked(true);
                        baseRefreshTokenRepository.saveAndFlush(rt);
                    });

            Cookie refreshTokenCookie = Arrays.stream(registerResult.getResponse().getCookies())
                    .filter(cookie -> cookieProperties.refreshTokenCookieName().equals(cookie.getName()))
                    .findFirst()
                    .orElse(null);

            mockMvc.perform(
                    post("/auth/refresh")
                            .cookie(refreshTokenCookie))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
        }

        // expired token
        @Test
        @DisplayName("Refresh token with expiry date before current time; 401 status code")
        @Transactional
        void refreshTokenWithExpiredToken() throws Exception {
            MvcResult registerResult = mockMvc.perform(
                    post("/auth/register")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(register)))
                    .andReturn();

            String refreshToken = extractTokenFromCookie(registerResult.getResponse(), cookieProperties.refreshTokenCookieName());
            baseRefreshTokenRepository.findById(UUID.fromString(refreshToken)).ifPresent(rt ->
            {
                rt.setExpiryDate(new Date(System.currentTimeMillis() - jwtProperties.refreshTokenExpiration()));
                baseRefreshTokenRepository.saveAndFlush(rt);
            });

            Cookie refreshTokenCookie = Arrays.stream(registerResult.getResponse().getCookies())
                    .filter(cookie -> cookieProperties.refreshTokenCookieName().equals(cookie.getName()))
                    .findFirst()
                    .orElse(null);

            mockMvc.perform(
                            post("/auth/refresh")
                                    .cookie(refreshTokenCookie))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
        }
    }

    private String extractTokenFromCookie(MockHttpServletResponse response, String cookieName) {
        if (response.getCookies().length == 0) return null;

        return Arrays.stream(response.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean cookieExists(MockHttpServletResponse response, String cookieName) {
        return Arrays.stream(response.getCookies())
                .anyMatch(cookie -> cookieName.equals(cookie.getName()));
    }
}
