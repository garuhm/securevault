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
import com.roadmap.securevault.service.helper.AccessJwtService;
import com.roadmap.securevault.service.helper.CookieService;
import com.roadmap.securevault.service.helper.RefreshJwtService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final UserService userService;
    private final AccessJwtService accessJwtService;
    private final RefreshJwtService refreshJwtService;
    private final CookieService cookieService;

    private final CookieProperties cookieProperties;

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
        User user = (User) userService.loadUserByUsername(credentials.username());

        if (user.getPassword() == null) {
            throw new BadCredentialsException(
                    "This account uses " + user.getOAuth2Links().iterator().next().getProvider() + " OAuth2 login"
            );
        }

        if (!passwordEncoder.matches(credentials.password(), user.getPassword())) {
            throw new BadCredentialsException("Username or password is incorrect.");
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
        String refreshToken = cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName());
        if (refreshToken != null) {
            refreshJwtService.revokeToken(UUID.fromString(refreshToken));
        }
        cookieService.clearTokenCookies(response);
    }

    @Transactional
    public void logoutAllSessions(HttpServletResponse response) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        refreshJwtService.revokeAllTokensForUser(user);
        cookieService.clearTokenCookies(response);
    }
}
