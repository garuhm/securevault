package com.roadmap.securevault.platform.controller;

import com.roadmap.securevault.common.annotation.ApiVersion;
import com.roadmap.securevault.common.controller.BaseAuthController;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.platform.dto.PlatformRegisterRequest;
import com.roadmap.securevault.platform.entity.PlatformUser;
import com.roadmap.securevault.platform.entity.enums.PlatformRole;
import com.roadmap.securevault.platform.repo.PlatformUserRepository;
import com.roadmap.securevault.platform.service.PlatformAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@ApiVersion("v1")
@RequestMapping("/platform/auth")
public class PlatformAuthController extends BaseAuthController<PlatformUser, PlatformUserRepository> {

    private final PlatformAuthService platformAuthService;

    public PlatformAuthController(PlatformAuthService platformAuthService) {
        super(platformAuthService);
        this.platformAuthService = platformAuthService;
    }

    @PostMapping("/register")
    @PreAuthorize("hasAuthority('PLATFORM_OWNER')")
    public ResponseEntity<Void> register(@Valid @RequestBody PlatformRegisterRequest credentials,
                                         @RequestParam PlatformRole role) {
        platformAuthService.register(credentials, role);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest credentials,
                                      HttpServletResponse response) {
        authService.login(credentials, response);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}