package com.roadmap.securevault.controller;

import com.roadmap.securevault.dto.user.RegisterRequest;
import com.roadmap.securevault.service.web.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")

@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest credentials) {
        authService.register(credentials);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logoutAllSessions();
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}