package com.roadmap.securevault.service;

import com.roadmap.securevault.dto.LoginRequest;
import com.roadmap.securevault.dto.RegisterRequest;
import com.roadmap.securevault.dto.AuthenticatedResponse;
import com.roadmap.securevault.entity.RefreshToken;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.exception.CredentialsTakenException;
import com.roadmap.securevault.mapper.UserMapper;
import com.roadmap.securevault.repo.RoleRepository;
import com.roadmap.securevault.repo.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final AccessJwtService accessJwtService;
    private final RefreshJwtService refreshJwtService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User with the username" + username + "not found"));
    }

    @Transactional
    public AuthenticatedResponse register(RegisterRequest request) {
        if(userRepository.existsByUsername(request.username())) {
            throw new CredentialsTakenException("Username already exists");
        }
        if(userRepository.existsByEmail(request.email())) {
            throw new CredentialsTakenException("Email already exists");
        }

        User user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.getRoles().add(roleRepository
                .findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new EntityNotFoundException("Role not found")));

        User savedUser = userRepository.save(user);
        return new AuthenticatedResponse(
                accessJwtService.generateAccessToken(savedUser),
                refreshJwtService.generateRefreshToken(savedUser).getId()
        );
    }

    @Transactional
    public AuthenticatedResponse login(LoginRequest request) {
        User user = userRepository
                .findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Username or password is incorrect"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials.");
        }

        RefreshToken refreshToken = refreshJwtService.findByUser(user);
        return new AuthenticatedResponse(
                accessJwtService.generateAccessToken(user),
                refreshToken.getId()
        );
    }

    @Transactional
    public AuthenticatedResponse refreshToken(UUID id) {
        RefreshJwtService.JwtRotationResult result = refreshJwtService.validateAndRotate(id);
        return new AuthenticatedResponse(
                result.accessToken(),
                result.refreshToken()
        );
    }

    @Transactional
    public void logout() {
        refreshJwtService.revokeAllTokensForUser(
                userRepository.findByUsername(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName())
                        .get());
    }
}
