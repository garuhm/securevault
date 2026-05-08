package com.roadmap.securevault.repo;

import com.roadmap.securevault.entity.Role;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.test_util.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UserAndRoleRepositoryTest extends AbstractPostgresIntegrationTest {
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("New user with a role has one linked row in user_roles")
    void testNewUserWithRoleHasOneLinkedRow() {
        User user = createUserWithUserRole("gabe", "email@email.com", "gabe");

        assertNotNull(user);
        assertNotNull(user.getId());
        assertNotNull(user.getUsername());
        assertNotNull(user.getEmail());
        assertNotNull(user.getPassword());
        assertNotNull(user.getRoles());
        assertEquals(1, user.getRoles().size());
        assertThat(user.getAuthorities().contains(RoleName.ROLE_USER));
    }

    @Test
    @DisplayName("New user with one or more null field is rejected")
    void testNewUserWithNullFieldIsRejected() {
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(
                User.builder()
                        .username(null)
                        .email("email@email.com")
                        .password(null)
                        .build()));
    }

    @Test
    @DisplayName("New user with duplicate email is rejected")
    void testNewUserWithDuplicateEmailIsRejected() {
        userRepository.saveAndFlush(
                User.builder()
                        .username("duplicateUser")
                        .email("duplicate@email.com")
                        .password("password")
                        .build());

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(
                User.builder()
                        .username("duplicateUser1")
                        .email("duplicate@email.com")
                        .password("password")
                        .build()));
    }

    @Test
    @DisplayName("New user with duplicate username is rejected")
    void testNewUserWithDuplicateUsernameIsRejected() {
        userRepository.saveAndFlush(User.builder()
                .username("duplicateUser")
                .email("duplicate@email.com")
                .password("password")
                .build());

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(
                User.builder()
                        .username("duplicateUser")
                        .email("duplicate2@email.com")
                        .password("password")
                        .build()));
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
}
