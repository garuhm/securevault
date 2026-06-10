package com.roadmap.securevault.controller;

import com.roadmap.securevault.dto.LoginRequest;
import com.roadmap.securevault.dto.RegisterRequest;
import com.roadmap.securevault.common.service.BaseAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")

@RequiredArgsConstructor
public class AuthController {
    private final BaseAuthService baseAuthService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest credentials,
                                         HttpServletResponse response) {
        baseAuthService.register(credentials, response);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest credentials,
                                      HttpServletResponse response) {
        baseAuthService.login(credentials, response);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(HttpServletRequest request,
                                             HttpServletResponse response) {
        baseAuthService.refreshToken(request, response);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response,
                                       @RequestParam(name = "all", required = false) boolean allSessions) {
        if(allSessions) baseAuthService.logoutAllSessions(response);
        else baseAuthService.logout(request, response);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
