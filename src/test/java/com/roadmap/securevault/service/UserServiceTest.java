package com.roadmap.securevault.service;

import com.roadmap.securevault.dto.RegisterRequest;
import com.roadmap.securevault.entity.Role;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.exception.CredentialsTakenException;
import com.roadmap.securevault.mapper.UserMapper;
import com.roadmap.securevault.repo.RoleRepository;
import com.roadmap.securevault.repo.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    private static final String USERNAME = "username";
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";

    @BeforeEach
    void globalSetUp() {
        user = User.builder()
                .username(USERNAME)
                .email(EMAIL)
                .password(PASSWORD)
                .build();
    }

    @Nested
    @DisplayName("Registration tests")
    class Register {
        private RegisterRequest request;

        @BeforeEach
        void setUp() {
            request = new RegisterRequest(USERNAME, EMAIL, PASSWORD);
        }

        @Test
        @DisplayName("Register a new user with valid credentials; successful")
        void registerNewUserWithValidCredentials() {
            // given
            when(userRepository.existsByUsername(request.username())).thenReturn(false);
            when(userRepository.existsByEmail(request.email())).thenReturn(false);

            // how to mock static methods
            try (MockedStatic<UserMapper> mockedUserMapper = mockStatic(UserMapper.class)) {
                // code that needs the mocked mapper
                mockedUserMapper.when(() -> UserMapper.toEntity(request)).thenReturn(user);

                when(passwordEncoder.encode(request.password())).thenReturn("{bcrypt}" + request.password());
                when(roleRepository.findByName(RoleName.ROLE_USER))
                        .thenReturn(Optional.of(Role.builder().name(RoleName.ROLE_USER).build()));
                when(userRepository.save(user)).thenReturn(user);

                // when
                userService.register(request);
            }

            //  then
            verify(userRepository, times(1)).save(user);
            verify(passwordEncoder, times(1)).encode(request.password());
            assertEquals("{bcrypt}" + request.password(), user.getPassword());
            assertEquals(1, user.getRoles().size());
            assertEquals(RoleName.ROLE_USER, user.getRoles().iterator().next().getName());
        }

        @Test
        @DisplayName("Register user with duplicate username; exception thrown")
        void registerUserWithDuplicateUsername() {
            when(userRepository.existsByUsername(request.username())).thenReturn(true);
            assertThrows(CredentialsTakenException.class, () -> userService.register(request));
        }

        @Test
        @DisplayName("Register user with duplicate email; exception thrown")
        void registerUserWithDuplicateEmail() {
            when(userRepository.existsByEmail(request.email())).thenReturn(true);
            assertThrows(CredentialsTakenException.class, () -> userService.register(request));
        }

        @Test
        @DisplayName("Register user with invalid role; exception thrown")
        void registerUserWithInvalidRole() {
            when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> userService.register(request));
        }
    }
}
