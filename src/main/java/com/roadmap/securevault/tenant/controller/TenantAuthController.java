package com.roadmap.securevault.tenant.controller;

import com.roadmap.securevault.common.controller.BaseAuthController;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.tenant.dto.tenant.TenantSetupRequest;
import com.roadmap.securevault.tenant.dto.invite.TenantUserRegisterRequest;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.repo.TenantUserRepository;
import com.roadmap.securevault.tenant.service.TenantAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/t/{companyCode}/auth")
public class TenantAuthController extends BaseAuthController<TenantUser, TenantUserRepository> {

    private final TenantAuthService tenantAuthService;

    public TenantAuthController(TenantAuthService tenantAuthService) {
        super(tenantAuthService);
        this.tenantAuthService = tenantAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody TenantUserRegisterRequest request,
                                         HttpServletResponse response,
                                         HttpServletRequest httpRequest) {
        tenantAuthService.register(request, response, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest credentials,
                                      HttpServletResponse response,
                                      HttpServletRequest request) {
        tenantAuthService.login(credentials, request, response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/setup")
    public ResponseEntity<Void> setup(@Valid @RequestBody TenantSetupRequest request,
                                      HttpServletResponse response,
                                      HttpServletRequest httpRequest) {
        tenantAuthService.setup(request, response, httpRequest);
        return ResponseEntity.ok().build();
    }
}