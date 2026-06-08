package com.roadmap.securevault.repo;

import com.roadmap.securevault.entity.RefreshToken;
import com.roadmap.securevault.entity.Role;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.test_util.testcontainers.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
@DisplayName("RefreshTokenRepository Data Jpa Tests")
public class RefreshTokenRepositoryDataJpaTest extends AbstractPostgresDataJpaTest {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    @DisplayName("User has a linked refresh token; successful")
    void testUserHasLinkedRefreshToken() {
        User user = createUserWithUserRole("username", "email", "password");

        RefreshToken refreshTokenToSave = RefreshToken.builder()
                .user(user)
                .expiryDate(new Date())
                .build();

        RefreshToken refreshToken = createRefreshToken(refreshTokenToSave, user);

        assertNotNull(refreshTokenRepository.findById(refreshToken.getId()));
        assertEquals(1, userRepository
                .findById(user.getId())
                .get()
                .getRefreshTokens()
                .size());
    }

    @Test
    @DisplayName("Revoking all by user revokes all; successful")
    void testRevokeAllByUserRevokesAll() {
        User user = createUserWithUserRole("username", "email", "password");
        RefreshToken refreshTokenToSave1 = RefreshToken.builder()
                .user(user)
                .expiryDate(new Date())
                .build();

        RefreshToken refreshTokenToSave2 = RefreshToken.builder()
                .user(user)
                .expiryDate(new Date())
                .build();

        RefreshToken refreshToken1 = createRefreshToken(refreshTokenToSave1, user);
        RefreshToken refreshToken2 = createRefreshToken(refreshTokenToSave2, user);

        refreshTokenRepository.revokeAllByUser(user);

        assertEquals(0, refreshTokenRepository
                .findAllByUserAndRevokedFalse(user)
                .size());
        userRepository.findById(user.getId())
                .get()
                .getRefreshTokens()
                .forEach(refreshToken -> {
                    assertTrue(refreshToken.isRevoked());
                });
    }

    @Test
    @DisplayName("Deleting all expired tokens results in no linked user tokens; successful")
    void testDeleteAllExpiredTokens() {
        User user = createUserWithUserRole("username", "email", "password");
        Date now = new Date();
        RefreshToken refreshTokenToSave1 = RefreshToken.builder()
                .user(user)
                .expiryDate(new Date(now.getTime() - 10000))
                .build();
        RefreshToken refreshTokenToSave2 = RefreshToken.builder()
                .user(user)
                .expiryDate(now)
                .build();

        RefreshToken refreshToken1 = createRefreshToken(refreshTokenToSave1, user);
        RefreshToken refreshToken2 = createRefreshToken(refreshTokenToSave2, user);

        testEntityManager.flush();
        testEntityManager.clear();

        refreshTokenRepository.deleteByExpiryDateBefore(now);
        
        assertEquals(1, refreshTokenRepository
                .findAllByUserAndRevokedFalse(user)
                .size());
        assertEquals(1, refreshTokenRepository.findAll().size());
    }

    private User createUserWithUserRole(String username, String email, String password) {
        Role userRole =
                roleRepository
                        .findByName(RoleName.ROLE_USER)
                        .orElseGet(
                                () ->
                                        roleRepository.saveAndFlush(
                                                Role.builder()
                                                        .name(RoleName.ROLE_USER)
                                                        .build()));

        return userRepository.saveAndFlush(
                User.builder()
                        .username(username)
                        .email(email)
                        .password(password)
                        .roles(Set.of(userRole))
                        .build());
    }

    private RefreshToken createRefreshToken(RefreshToken refreshToken, User user) {
        user.getRefreshTokens()
                .add(refreshToken);

        return refreshTokenRepository.saveAndFlush(refreshToken);
    }
}
