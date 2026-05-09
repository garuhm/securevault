package com.roadmap.securevault.service;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.dto.LoginRequest;
import com.roadmap.securevault.dto.RegisterRequest;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.exception.CredentialsTakenException;
import com.roadmap.securevault.mapper.UserMapper;
import com.roadmap.securevault.repo.RoleRepository;
import com.roadmap.securevault.repo.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final CookieService cookieService;
    private final CookieProperties cookieProperties;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User with the username" + username + "not found"));
    }

    @Transactional
    public void register(RegisterRequest credentials,
                         HttpServletResponse response) {
        if(userRepository.existsByUsername(credentials.username())) {
            throw new CredentialsTakenException("Username already exists");
        }
        if(userRepository.existsByEmail(credentials.email())) {
            throw new CredentialsTakenException("Email already exists");
        }

        User user = UserMapper.toEntity(credentials);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.getRoles().add(roleRepository
                .findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new EntityNotFoundException("Role not found")));

        User savedUser = userRepository.save(user);
        cookieService.addTokenCookies(
                response,
                accessJwtService.generateAccessToken(savedUser),
                refreshJwtService.generateRefreshToken(savedUser).getId().toString()
        );
    }

    @Transactional
    public void login(LoginRequest credentials,
                                       HttpServletResponse response) {
        User user = userRepository
                .findByUsername(credentials.username())
                .orElseThrow(() -> new BadCredentialsException("Username or password is incorrect"));

        if (!passwordEncoder.matches(credentials.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials.");
        }

        cookieService.addTokenCookies(
                response,
                accessJwtService.generateAccessToken(user),
                refreshJwtService.generateRefreshToken(user).getId().toString()
        );
    }

    @Transactional
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        RefreshJwtService.JwtRotationResult result = refreshJwtService.validateAndRotate(request);
        cookieService.addTokenCookies(response, result.accessToken(), result.refreshToken().toString());
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        User user = userRepository.findByUsername(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        refreshJwtService.revokeToken(
                UUID.fromString(
                        cookieService
                                .extractTokenFromCookie(
                                        request, cookieProperties
                                                .refreshTokenCookieName())));
        cookieService.clearTokenCookies(response);
    }
}
