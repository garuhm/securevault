package com.roadmap.securevault.service;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.JwtProperties;
import com.roadmap.securevault.dto.LoginRequest;
import com.roadmap.securevault.dto.RegisterRequest;
import com.roadmap.securevault.entity.Role;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.exception.CredentialsTakenException;
import com.roadmap.securevault.mapper.UserMapper;
import com.roadmap.securevault.repo.RoleRepository;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.service.helper.AccessJwtService;
import com.roadmap.securevault.service.helper.CookieService;
import com.roadmap.securevault.service.helper.RefreshJwtService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CookieService cookieService;
    @Mock
    private AccessJwtService accessJwtService;
    @Mock
    private RefreshJwtService refreshJwtService;
    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    private User user;

    private static final String USERNAME = "username";
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private final JwtProperties jwtProperties = new JwtProperties(
            "vTpTSehgudwctoSkCRB3HCdrW8NJBRA1s02pf7TXh9E",
            900000L,
            604800000L
    );

    private final CookieProperties cookieProperties = new CookieProperties(
            "securevault-access-token",
            "securevault-refresh-token",
            "/auth/refresh",
            "securevault-oauth2-request",
            180,
            "securevault-oauth2-linking-request"

    );

    @BeforeEach
    void globalSetUp() {
        user = User.builder()
                .username(USERNAME)
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("Registration tests")
    class Register {
        private RegisterRequest credentials;

        @BeforeEach
        void setUp() {
            credentials = new RegisterRequest(USERNAME, EMAIL, PASSWORD);
        }

        @Test
        @DisplayName("Register a new user with valid credentials; successful")
        void registerNewUserWithValidCredentials() {
            // given
            when(userRepository.existsByUsername(credentials.username())).thenReturn(false);
            when(userRepository.existsByEmail(credentials.email())).thenReturn(false);
            when(refreshJwtService.generateRefreshToken(any())).thenReturn(
                    com.roadmap.securevault.entity.RefreshToken.builder()
                            .id(UUID.randomUUID())
                            .user(user)
                            .expiryDate(new Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration()))
                            .build());

            // how to mock static methods
            try (MockedStatic<UserMapper> mockedUserMapper = mockStatic(UserMapper.class)) {
                // code that needs the mocked mapper
                mockedUserMapper.when(() -> UserMapper.toEntity(credentials)).thenReturn(user);

                when(passwordEncoder.encode(credentials.password())).thenReturn("{bcrypt}" + credentials.password());
                when(roleRepository.findByName(RoleName.ROLE_USER))
                        .thenReturn(Optional.of(Role.builder().name(RoleName.ROLE_USER).build()));
                when(userRepository.saveAndFlush(user)).thenReturn(user);

                // when
                authService.register(credentials, response);
            }

            //  then
            verify(userRepository, times(1)).saveAndFlush(user);
            verify(passwordEncoder, times(1)).encode(credentials.password());
            verify(refreshJwtService, times(1)).generateRefreshToken(any());
            assertEquals("{bcrypt}" + credentials.password(), user.getPassword());
            assertEquals(1, user.getRoles().size());
            assertEquals(RoleName.ROLE_USER, user.getRoles().iterator().next().getName());

            Arrays.stream(response.getCookies()).filter(
                    cookie -> cookie.getName()
                            .equals(cookieProperties.accessTokenCookieName()))
                    .findFirst()
                    .ifPresent(
                            cookie -> {
                                String accessToken = cookieService.extractTokenFromCookie(request, cookie.getName());
                                assertNotNull(cookie.getValue());
                                assertTrue(accessJwtService.isTokenValid(accessToken, user));
                                assertEquals("/", cookie.getPath());
                                assertEquals(jwtProperties.accessTokenExpiration() / 1000, cookie.getMaxAge());
                                assertTrue(cookie.isHttpOnly());
                                assertFalse(cookie.getSecure());
                            });

            Arrays.stream(response.getCookies()).filter(
                            cookie -> cookie.getName()
                                    .equals(cookieProperties.refreshTokenCookieName()))
                    .findFirst()
                    .ifPresent(
                            cookie -> {
                                assertNotNull(cookie.getValue());
                                assertEquals(cookieProperties.refreshTokenCookiePath(), cookie.getPath());
                                assertEquals(jwtProperties.refreshTokenExpiration() / 1000, cookie.getMaxAge());
                                assertTrue(cookie.isHttpOnly());
                                assertFalse(cookie.getSecure());
                            });
        }

        @Test
        @DisplayName("Register user with duplicate username; exception thrown")
        void registerUserWithDuplicateUsername() {
            when(userRepository.existsByUsername(credentials.username())).thenReturn(true);
            assertThrows(CredentialsTakenException.class, () -> authService.register(credentials, response));
        }

        @Test
        @DisplayName("Register user with duplicate email; exception thrown")
        void registerUserWithDuplicateEmail() {
            when(userRepository.existsByEmail(credentials.email())).thenReturn(true);
            assertThrows(CredentialsTakenException.class, () -> authService.register(credentials, response));
        }

        @Test
        @DisplayName("Register user with invalid role; exception thrown")
        void registerUserWithInvalidRole() {
            when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> authService.register(credentials, response));
        }
    }

    @Nested
    @DisplayName("Login tests")
    class Login {
        private LoginRequest credentials;

        @BeforeEach
        void setUp() { credentials = new LoginRequest(USERNAME, PASSWORD); }

        @Test
        @DisplayName("Login; successful")
        void login() {
            when(userService.loadUserByUsername(credentials.username())).thenReturn(user);
            when(passwordEncoder.matches(credentials.password(), user.getPassword())).thenReturn(true);
            when(refreshJwtService.generateRefreshToken(any())).thenReturn(
                    com.roadmap.securevault.entity.RefreshToken.builder()
                            .id(UUID.randomUUID())
                            .user(user)
                            .expiryDate(new Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration()))
                            .build());

            authService.login(credentials, response);

            verify(userService, times(1)).loadUserByUsername(credentials.username());
            verify(passwordEncoder, times(1)).matches(credentials.password(), user.getPassword());
            verify(refreshJwtService, times(1)).generateRefreshToken(any());

            Arrays.stream(response.getCookies()).filter(
                            cookie -> cookie.getName()
                                    .equals(cookieProperties.accessTokenCookieName()))
                    .findFirst()
                    .ifPresent(
                            cookie -> {
                                String accessToken = cookieService.extractTokenFromCookie(request, cookie.getName());
                                assertNotNull(cookie.getValue());
                                assertTrue(accessJwtService.isTokenValid(accessToken, user));
                                assertEquals("/", cookie.getPath());
                                assertEquals(jwtProperties.accessTokenExpiration() / 1000, cookie.getMaxAge());
                                assertTrue(cookie.isHttpOnly());
                                assertFalse(cookie.getSecure());
                            });

            Arrays.stream(response.getCookies()).filter(
                            cookie -> cookie.getName()
                                    .equals(cookieProperties.refreshTokenCookieName()))
                    .findFirst()
                    .ifPresent(
                            cookie -> {
                                assertNotNull(cookie.getValue());
                                assertEquals(cookieProperties.refreshTokenCookiePath(), cookie.getPath());
                                assertEquals(jwtProperties.refreshTokenExpiration() / 1000, cookie.getMaxAge());
                                assertTrue(cookie.isHttpOnly());
                                assertFalse(cookie.getSecure());
                            });
        }

        @Test
        @DisplayName("Login with invalid username; exception thrown")
        void loginWithInvalidUsername() {
            when(userService.loadUserByUsername(credentials.username())).thenThrow(UsernameNotFoundException.class);
            assertThrows(UsernameNotFoundException.class, () -> authService.login(credentials, response));
        }

        @Test
        @DisplayName("Login with invalid password; exception thrown")
        void loginWithInvalidPassword() {
            when(userService.loadUserByUsername(credentials.username())).thenReturn(user);
            when(passwordEncoder.matches(credentials.password(), user.getPassword())).thenReturn(false);
            assertThrows(BadCredentialsException.class, () -> authService.login(credentials, response));
        }
    }

    @Nested
    @DisplayName("Refresh token tests")
    class RefreshToken {
        @Test
        @DisplayName("Refresh with valid refresh token; successful")
        void refreshWithValidRefreshToken() {
            when(refreshJwtService.validateAndRotate(any())).thenReturn(
                    new RefreshJwtService.JwtRotationResult("accessToken", UUID.randomUUID(), user));
            doAnswer(
                    invocation -> {
                        response.addCookie(new Cookie(cookieProperties.accessTokenCookieName(), invocation.getArgument(1)));
                        response.addCookie(new Cookie(cookieProperties.refreshTokenCookieName(), invocation.getArgument(2)));
                        return null;
                    }
            ).when(cookieService).addTokenCookies(eq(response), eq("accessToken"), any());

            authService.refreshToken(request, response);
            verify(cookieService, times(1)).addTokenCookies(eq(response), eq("accessToken"), any());

            assertEquals(2, response.getCookies().length);
        }
    }
}
