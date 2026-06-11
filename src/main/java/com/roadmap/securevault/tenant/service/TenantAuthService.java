package com.roadmap.securevault.tenant.service;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.common.exception.CredentialsTakenException;
import com.roadmap.securevault.common.exception.InvalidBootstrapTokenException;
import com.roadmap.securevault.common.service.AccessJwtService;
import com.roadmap.securevault.common.service.BaseAuthService;
import com.roadmap.securevault.common.service.CookieService;
import com.roadmap.securevault.tenant.dto.tenant.TenantSetupRequest;
import com.roadmap.securevault.tenant.entity.Tenant;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import com.roadmap.securevault.tenant.mapper.TenantUserMapper;
import com.roadmap.securevault.tenant.repo.TenantUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class TenantAuthService extends BaseAuthService<TenantUser, TenantUserRepository> {
    private final BootstrapTokenService bootstrapTokenService;

    public TenantAuthService(TenantUserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               TenantUserService userService,
                               AccessJwtService accessJwtService,
                               TenantRefreshJwtService baseRefreshJwtService,
                               CookieService cookieService,
                               CookieProperties cookieProperties,
                               BootstrapTokenService bootstrapTokenService) {
        super(userRepository, passwordEncoder, userService,
                accessJwtService, baseRefreshJwtService, cookieService, cookieProperties);
        this.bootstrapTokenService = bootstrapTokenService;
    }

    @Transactional
    public void login(LoginRequest credentials, HttpServletRequest request, HttpServletResponse response) {
        TenantUser user = (TenantUser) userService.loadUserByUsername(credentials.username());
        if (!passwordEncoder.matches(credentials.password(), user.getPassword())) {
            throw new BadCredentialsException("Username or password is incorrect.");
        }

        Tenant tenant = (Tenant) request.getAttribute("tenant");

        Map<String, Object> extraClaims = Map.of(
                "tenantId", tenant.getId().toString(),
                "companyCode", tenant.getCompanyCode()
        );

        cookieService.addTokenCookies(
                response,
                accessJwtService.generateAccessToken(user, extraClaims),
                baseRefreshJwtService.generateRefreshToken(user).getId().toString()
        );
    }

    @Transactional
    public void setup(TenantSetupRequest request, HttpServletResponse response, HttpServletRequest httpRequest) {
        Tenant tenant = (Tenant) httpRequest.getAttribute("tenant");

        // 1. validate credentials
        if (userRepository.existsByUsername(request.username())) {
            throw new CredentialsTakenException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new CredentialsTakenException("Email already exists");
        }

        // 2. verify token exists without consuming
        UUID tenantId = bootstrapTokenService.peekBootstrapToken(request.token())
                .orElseThrow(() -> new InvalidBootstrapTokenException("Invalid or expired bootstrap token"));

        // 3. verify tenant matches
        if (!tenant.getId().equals(tenantId)) {
            throw new InvalidBootstrapTokenException("Bootstrap token does not match tenant");
        }

        // 4. consume token (irreversible)
        bootstrapTokenService.consumeBootstrapToken(request.token());

        // 5. save user
        TenantUser owner = TenantUserMapper.toEntity(request);
        owner.setPassword(passwordEncoder.encode(owner.getPassword()));
        owner.setRole(TenantRole.TENANT_OWNER);
        TenantUser saved = userRepository.save(owner);

        // 6. issue cookies
        Map<String, Object> extraClaims = Map.of(
                "tenantId", tenant.getId().toString(),
                "companyCode", tenant.getCompanyCode()
        );
        cookieService.addTokenCookies(
                response,
                accessJwtService.generateAccessToken(saved, extraClaims),
                baseRefreshJwtService.generateRefreshToken(saved).getId().toString()
        );
    }
}
