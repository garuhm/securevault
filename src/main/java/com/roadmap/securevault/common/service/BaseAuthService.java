package com.roadmap.securevault.common.service;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.dto.JwtRotationResult;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.common.entity.BaseUser;
import com.roadmap.securevault.common.repo.BaseUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

public abstract class BaseAuthService<U extends BaseUser & UserDetails, R extends BaseUserRepository<U> & JpaSpecificationExecutor<U>> {

    protected final R userRepository;
    protected final PasswordEncoder passwordEncoder;
    protected final BaseUserService<U, R, ?, ?> userService;
    protected final AccessJwtService accessJwtService;
    protected final BaseRefreshJwtService<U, ?> baseRefreshJwtService;
    protected final CookieService cookieService;
    protected final CookieProperties cookieProperties;

    protected BaseAuthService(R userRepository,
                              PasswordEncoder passwordEncoder,
                              BaseUserService<U, R, ?, ?> userDetailsService,
                              AccessJwtService accessJwtService,
                              BaseRefreshJwtService<U, ?> baseRefreshJwtService,
                              CookieService cookieService,
                              CookieProperties cookieProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userDetailsService;
        this.accessJwtService = accessJwtService;
        this.baseRefreshJwtService = baseRefreshJwtService;
        this.cookieService = cookieService;
        this.cookieProperties = cookieProperties;
    }

    @Transactional
    public void login(LoginRequest credentials, HttpServletResponse response) {
        U user = (U) userService.loadUserByUsername(credentials.username());
        if (!passwordEncoder.matches(credentials.password(), user.getPassword())) {
            throw new BadCredentialsException("Username or password is incorrect.");
        }
        cookieService.addTokenCookies(
                response,
                accessJwtService.generateAccessToken(user),
                baseRefreshJwtService.generateRefreshToken(user).getId().toString()
        );
    }

    @Transactional
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        JwtRotationResult<U> result = baseRefreshJwtService.validateAndRotate(request);
        cookieService.addTokenCookies(response, result.accessToken(), result.refreshToken().toString());
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.extractTokenFromCookie(request, cookieProperties.refreshTokenCookieName());
        if (refreshToken != null) {
            baseRefreshJwtService.revokeToken(UUID.fromString(refreshToken));
        }
        cookieService.clearTokenCookies(response);
    }

    @Transactional
    public void logoutAllSessions(HttpServletResponse response) {
        U user = (U) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        baseRefreshJwtService.revokeAllTokensForUser(user);
        cookieService.clearTokenCookies(response);
    }
}