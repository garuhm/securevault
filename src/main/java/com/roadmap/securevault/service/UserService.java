package com.roadmap.securevault.service;

import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService  {
    private final UserRepository userRepository;

    public User syncFromJwt(Jwt jwt) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
