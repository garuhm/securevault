package com.roadmap.securevault.service;

import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // WILL BE REWRITTEN TO LISTEN FOR KAFKA
    // INSTEAD OF DB-CALL-PER-UPDATE
    @Transactional
    public User syncFromJwt(Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");

        return userRepository.findById(id)
                .map(existing -> updateIfChanged(existing, username, email))
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .id(id)
                                .username(username)
                                .email(email)
                                .build()
                ));
    }

    private User updateIfChanged(User user, String username, String email) {
        boolean changed = false;
        if (!user.getUsername().equals(username)) {
            user.setUsername(username);
            changed = true;
        }
        if (!user.getEmail().equals(email)) {
            user.setEmail(email);
            changed = true;
        }
        return changed ? userRepository.save(user) : user;
    }
}