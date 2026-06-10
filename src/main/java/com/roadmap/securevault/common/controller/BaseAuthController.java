package com.roadmap.securevault.common.controller;

import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.common.entity.BaseUser;
import com.roadmap.securevault.common.repo.BaseUserRepository;
import com.roadmap.securevault.common.service.BaseAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

public abstract class BaseAuthController<U extends BaseUser & UserDetails, R extends BaseUserRepository<U>> {

    protected final BaseAuthService<U, R> authService;

    protected BaseAuthController(BaseAuthService<U, R> authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest credentials,
                                      HttpServletResponse response) {
        authService.login(credentials, response);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(HttpServletRequest request,
                                             HttpServletResponse response) {
        authService.refreshToken(request, response);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response,
                                       @RequestParam(name = "all", required = false) boolean allSessions) {
        if (allSessions) authService.logoutAllSessions(response);
        else authService.logout(request, response);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}