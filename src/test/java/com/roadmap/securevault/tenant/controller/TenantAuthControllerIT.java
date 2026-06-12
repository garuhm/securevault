package com.roadmap.securevault.tenant.controller;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.common.service.AccessJwtService;
import com.roadmap.securevault.common.util.ApiVersioningResolver;
import com.roadmap.securevault.multitenancy.TenantContext;
import com.roadmap.securevault.tenant.dto.invite.TenantUserRegisterRequest;
import com.roadmap.securevault.tenant.dto.tenant.TenantSetupRequest;
import com.roadmap.securevault.tenant.entity.InviteCode;
import com.roadmap.securevault.tenant.entity.Tenant;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import com.roadmap.securevault.tenant.entity.enums.TenantStatus;
import com.roadmap.securevault.tenant.repo.InviteCodeRepository;
import com.roadmap.securevault.tenant.repo.TenantRefreshTokenRepository;
import com.roadmap.securevault.tenant.repo.TenantRepository;
import com.roadmap.securevault.tenant.repo.TenantUserRepository;
import com.roadmap.securevault.tenant.service.BootstrapTokenService;
import com.roadmap.securevault.tenant.service.InviteTokenService;
import com.roadmap.securevault.tenant.service.TenantSchemaInitializer;
import com.roadmap.securevault.test_util.testcontainers.AbstractIT;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("TenantAuthController Integration Tests")
class TenantAuthControllerIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired TenantRepository tenantRepository;
    @Autowired TenantUserRepository tenantUserRepository;
    @Autowired TenantRefreshTokenRepository tenantRefreshTokenRepository;
    @Autowired InviteCodeRepository inviteCodeRepository;
    @Autowired TenantSchemaInitializer tenantSchemaInitializer;
    @Autowired BootstrapTokenService bootstrapTokenService;
    @Autowired InviteTokenService inviteTokenService;
    @Autowired AccessJwtService accessJwtService;
    @Autowired CookieProperties cookieProperties;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager transactionManager;

    private static final String COMPANY_CODE = "acme";
    private static final String OWNER_USERNAME = "owner";
    private static final String OWNER_PASSWORD = "Owner@1234";

    private static final String LOGIN_URL =
            ApiVersioningResolver.resolve(TenantAuthController.class, "login", "/t/{code}/auth/login");
    private static final String REFRESH_URL =
            ApiVersioningResolver.resolve(TenantAuthController.class, "refreshToken", "/t/{code}/auth/refresh");
    private static final String LOGOUT_URL =
            ApiVersioningResolver.resolve(TenantAuthController.class, "logout", "/t/{code}/auth/logout");
    private static final String SETUP_URL =
            ApiVersioningResolver.resolve(TenantAuthController.class, "setup", "/t/{code}/auth/setup");
    private static final String REGISTER_URL =
            ApiVersioningResolver.resolve(TenantAuthController.class, "register", "/t/{code}/auth/register");

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .companyName("Acme Corp")
                .companyCode(COMPANY_CODE)
                .ownerEmail("owner@" + COMPANY_CODE + ".com")
                .build();
        tenant = tenantRepository.saveAndFlush(tenant);
        tenant.setSchemaName("tenant_" + tenant.getId().toString().replace("-", ""));
        tenant.setStatus(TenantStatus.APPROVED);
        tenant = tenantRepository.saveAndFlush(tenant);

        tenantSchemaInitializer.migrateSchema(tenant.getSchemaName());

        inTenantTransaction(() -> {
            tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .username(OWNER_USERNAME)
                    .email("owner@" + COMPANY_CODE + ".com")
                    .password(passwordEncoder.encode(OWNER_PASSWORD))
                    .role(TenantRole.TENANT_OWNER)
                    .build());
        });
    }

    @AfterEach
    void tearDown() {
        inTenantTransaction(() -> {
            tenantRefreshTokenRepository.deleteAll();
            inviteCodeRepository.deleteAll();
            tenantUserRepository.deleteAll();
        });
        tenantRepository.deleteAll();
        dropAllTenantSchemas();
    }

    void dropAllTenantSchemas() {
        List<String> schemas = jdbcTemplate.queryForList(
                "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'tenant\\_%'",
                String.class);

        for (String schema : schemas) {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    /**
     * Runs the given action in a fresh transaction with TenantContext set BEFORE the
     * transaction (and its connection/tenant resolution) begins. Use this for any
     * repository access against the tenant schema outside the MockMvc request cycle.
     */
    void inTenantTransaction(Runnable action) {
        inTenantTransaction(() -> {
            action.run();
            return null;
        });
    }

    <T> T inTenantTransaction(Supplier<T> action) {
        TenantContext.setTenantSchema(tenant.getSchemaName());
        try {
            return new TransactionTemplate(transactionManager).execute(status -> action.get());
        } finally {
            TenantContext.clear();
        }
    }

    MvcResult login(String companyCode, String username, String password) throws Exception {
        return mockMvc.perform(post(LOGIN_URL, companyCode)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andReturn();
    }

    Cookie extractCookie(MvcResult result, String cookieName) {
        return Arrays.stream(result.getResponse().getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .findFirst()
                .orElse(null);
    }

    String extractCookieValue(MvcResult result, String cookieName) {
        return Arrays.stream(result.getResponse().getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    boolean cookieExists(MvcResult result, String cookieName) {
        return Arrays.stream(result.getResponse().getCookies())
                .anyMatch(c -> cookieName.equals(c.getName()));
    }

    TenantUser tenantOwner() {
        return inTenantTransaction(() -> tenantUserRepository.findByUsername(OWNER_USERNAME).get());
    }

    @Nested
    @DisplayName("Login")
    class Login {

        @Test
        @DisplayName("Valid credentials → 200 + cookies with tenantId claim")
        void loginSuccess() throws Exception {
            MvcResult result = login(COMPANY_CODE, OWNER_USERNAME, OWNER_PASSWORD);

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(cookieExists(result, cookieProperties.accessTokenCookieName())).isTrue();
            assertThat(cookieExists(result, cookieProperties.refreshTokenCookieName())).isTrue();

            String accessToken = extractCookieValue(result, cookieProperties.accessTokenCookieName());
            assertThat(accessJwtService.extractClaim(accessToken, "tenantId")).isEqualTo(tenant.getId().toString());
            assertThat(accessJwtService.extractClaim(accessToken, "companyCode")).isEqualTo(COMPANY_CODE);

            inTenantTransaction(() ->
                    assertThat(tenantRefreshTokenRepository.findAllByUserAndRevokedFalse(tenantOwner())).hasSize(1));
        }

        @Test
        @DisplayName("Wrong password → 401")
        void wrongPassword() throws Exception {
            mockMvc.perform(post(LOGIN_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(OWNER_USERNAME, "WrongPass@1"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unknown user → 401")
        void unknownUser() throws Exception {
            mockMvc.perform(post(LOGIN_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest("nobody", OWNER_PASSWORD))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unknown tenant → 404")
        void unknownTenant() throws Exception {
            mockMvc.perform(post(LOGIN_URL, "doesnotexist")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(OWNER_USERNAME, OWNER_PASSWORD))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Suspended tenant → 403")
        void suspendedTenant() throws Exception {
            tenant.setStatus(TenantStatus.SUSPENDED);
            tenantRepository.saveAndFlush(tenant);

            mockMvc.perform(post(LOGIN_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(OWNER_USERNAME, OWNER_PASSWORD))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Refresh")
    class Refresh {

        @Test
        @DisplayName("Valid token → 200 + new cookies")
        void refreshSuccess() throws Exception {
            MvcResult loginResult = login(COMPANY_CODE, OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult refreshResult = mockMvc.perform(post(REFRESH_URL, COMPANY_CODE)
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName()))
                            .cookie(extractCookie(loginResult, cookieProperties.refreshTokenCookieName())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(cookieExists(refreshResult, cookieProperties.accessTokenCookieName())).isTrue();
            assertThat(cookieExists(refreshResult, cookieProperties.refreshTokenCookieName())).isTrue();

            inTenantTransaction(() -> {
                assertThat(tenantRefreshTokenRepository.findAll()).hasSize(2);
                assertThat(tenantRefreshTokenRepository.findAllByUserAndRevokedFalse(tenantOwner())).hasSize(1);
            });
        }

        @Test
        @DisplayName("No cookie → 401")
        void noCookie() throws Exception {
            mockMvc.perform(post(REFRESH_URL, COMPANY_CODE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Revoked token → 401")
        void revokedToken() throws Exception {
            MvcResult loginResult = login(COMPANY_CODE, OWNER_USERNAME, OWNER_PASSWORD);
            String refreshTokenValue = extractCookieValue(loginResult, cookieProperties.refreshTokenCookieName());

            inTenantTransaction(() ->
                    tenantRefreshTokenRepository.findById(UUID.fromString(refreshTokenValue))
                            .ifPresent(rt -> {
                                rt.setRevoked(true);
                                tenantRefreshTokenRepository.saveAndFlush(rt);
                            }));

            mockMvc.perform(post(REFRESH_URL, COMPANY_CODE)
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName()))
                            .cookie(extractCookie(loginResult, cookieProperties.refreshTokenCookieName())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Expired token → 401")
        void expiredToken() throws Exception {
            MvcResult loginResult = login(COMPANY_CODE, OWNER_USERNAME, OWNER_PASSWORD);
            String refreshTokenValue = extractCookieValue(loginResult, cookieProperties.refreshTokenCookieName());

            inTenantTransaction(() ->
                    tenantRefreshTokenRepository.findById(UUID.fromString(refreshTokenValue))
                            .ifPresent(rt -> {
                                rt.setExpiryDate(new Date(System.currentTimeMillis() - 1000));
                                tenantRefreshTokenRepository.saveAndFlush(rt);
                            }));

            mockMvc.perform(post(REFRESH_URL, COMPANY_CODE)
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName()))
                            .cookie(extractCookie(loginResult, cookieProperties.refreshTokenCookieName())))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Logout")
    class Logout {

        @Test
        @DisplayName("Valid → 200 + cookies cleared")
        void logoutSuccess() throws Exception {
            MvcResult loginResult = login(COMPANY_CODE, OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult logoutResult = mockMvc.perform(post(LOGOUT_URL, COMPANY_CODE)
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName()))
                            .cookie(extractCookie(loginResult, cookieProperties.refreshTokenCookieName())))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie clearedAccess = Arrays.stream(logoutResult.getResponse().getCookies())
                    .filter(c -> cookieProperties.accessTokenCookieName().equals(c.getName()))
                    .findFirst().orElse(null);
            assertThat(clearedAccess).isNotNull();
            assertThat(clearedAccess.getMaxAge()).isZero();

            inTenantTransaction(() ->
                    assertThat(tenantRefreshTokenRepository.findAllByUserAndRevokedFalse(tenantOwner())).isEmpty());
        }

        @Test
        @DisplayName("Logout all → 200 + all revoked")
        void logoutAll() throws Exception {
            login(COMPANY_CODE, OWNER_USERNAME, OWNER_PASSWORD);
            MvcResult login2 = login(COMPANY_CODE, OWNER_USERNAME, OWNER_PASSWORD);

            mockMvc.perform(post(LOGOUT_URL, COMPANY_CODE)
                            .queryParam("all", "true")
                            .cookie(extractCookie(login2, cookieProperties.accessTokenCookieName()))
                            .cookie(extractCookie(login2, cookieProperties.refreshTokenCookieName())))
                    .andExpect(status().isOk());

            inTenantTransaction(() ->
                    assertThat(tenantRefreshTokenRepository.findAllByUserAndRevokedFalse(tenantOwner())).isEmpty());
        }
    }

    @Nested
    @DisplayName("Setup")
    class Setup {

        String generateBootstrapToken() {
            return bootstrapTokenService.generateToken(tenant.getId());
        }

        @Test
        @DisplayName("Valid bootstrap token → 200 + cookies")
        void validBootstrapToken() throws Exception {
            String token = generateBootstrapToken();

            TenantSetupRequest request = new TenantSetupRequest(
                    token, "setupowner", "setupowner@" + COMPANY_CODE + ".com", "Setup@1234");

            MvcResult result = mockMvc.perform(post(SETUP_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn();

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(cookieExists(result, cookieProperties.accessTokenCookieName())).isTrue();
            assertThat(cookieExists(result, cookieProperties.refreshTokenCookieName())).isTrue();

            inTenantTransaction(() ->
                    assertThat(tenantUserRepository.existsByUsername("setupowner")).isTrue());
        }

        @Test
        @DisplayName("Invalid token → 401")
        void invalidToken() throws Exception {
            TenantSetupRequest request = new TenantSetupRequest(
                    "not-a-real-token", "setupowner2", "setupowner2@" + COMPANY_CODE + ".com", "Setup@1234");

            mockMvc.perform(post(SETUP_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token tenant mismatch → 401")
        void tokenTenantMismatch() throws Exception {
            Tenant otherTenant = Tenant.builder()
                    .companyName("Other Corp")
                    .companyCode("other")
                    .ownerEmail("owner@other.com")
                    .build();
            otherTenant = tenantRepository.saveAndFlush(otherTenant);
            otherTenant.setSchemaName("tenant_" + otherTenant.getId().toString().replace("-", ""));
            otherTenant.setStatus(TenantStatus.APPROVED);
            tenantRepository.saveAndFlush(otherTenant);

            String token = bootstrapTokenService.generateToken(otherTenant.getId());

            TenantSetupRequest request = new TenantSetupRequest(
                    token, "setupowner3", "setupowner3@" + COMPANY_CODE + ".com", "Setup@1234");

            mockMvc.perform(post(SETUP_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            tenantRepository.delete(otherTenant);
        }

        @Test
        @DisplayName("Duplicate username → 409")
        void duplicateUsername() throws Exception {
            String token = generateBootstrapToken();

            TenantSetupRequest request = new TenantSetupRequest(
                    token, OWNER_USERNAME, "different@" + COMPANY_CODE + ".com", "Setup@1234");

            mockMvc.perform(post(SETUP_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Duplicate email → 409")
        void duplicateEmail() throws Exception {
            String token = generateBootstrapToken();

            TenantSetupRequest request = new TenantSetupRequest(
                    token, "differentusername", "owner@" + COMPANY_CODE + ".com", "Setup@1234");

            mockMvc.perform(post(SETUP_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Register")
    class Register {

        InviteCode seedInvite(String email, boolean used) {
            return inTenantTransaction(() -> {
                TenantUser creator = tenantUserRepository.findByUsername(OWNER_USERNAME).get();

                InviteCode invite = InviteCode.builder()
                        .createdBy(creator)
                        .inviteeEmail(email)
                        .role(TenantRole.TENANT_MEMBER)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .build();

                invite = inviteCodeRepository.saveAndFlush(invite);

                String token = inviteTokenService.generateToken(invite.getId());
                invite.setCode(token);

                if (used) {
                    invite.setUsedAt(LocalDateTime.now());
                    inviteTokenService.consumeToken(token);
                }

                return inviteCodeRepository.saveAndFlush(invite);
            });
        }

        @Test
        @DisplayName("Valid invite token → 201 + cookies")
        void validInviteToken() throws Exception {
            InviteCode invite = seedInvite("invitee@test.com", false);

            TenantUserRegisterRequest request = new TenantUserRegisterRequest(
                    invite.getCode(), "inviteduser", "invitee@test.com", "Invited@1234");

            MvcResult result = mockMvc.perform(post(REGISTER_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn();

            assertThat(result.getResponse().getStatus()).isEqualTo(201);
            assertThat(cookieExists(result, cookieProperties.accessTokenCookieName())).isTrue();
            assertThat(cookieExists(result, cookieProperties.refreshTokenCookieName())).isTrue();

            inTenantTransaction(() -> {
                assertThat(tenantUserRepository.existsByUsername("inviteduser")).isTrue();
                assertThat(tenantUserRepository.findByUsername("inviteduser").get().getRole())
                        .isEqualTo(TenantRole.TENANT_MEMBER);
            });
        }

        @Test
        @DisplayName("Invalid token → 401")
        void invalidToken() throws Exception {
            TenantUserRegisterRequest request = new TenantUserRegisterRequest(
                    "not-a-real-token", "baduser", "bad@test.com", "Invited@1234");

            mockMvc.perform(post(REGISTER_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Used token → 401")
        void usedToken() throws Exception {
            InviteCode invite = seedInvite("used@test.com", true);

            TenantUserRegisterRequest request = new TenantUserRegisterRequest(
                    invite.getCode(), "useduser", "used@test.com", "Invited@1234");

            mockMvc.perform(post(REGISTER_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Duplicate username → 409")
        void duplicateUsername() throws Exception {
            InviteCode invite = seedInvite("dupuser@test.com", false);

            TenantUserRegisterRequest request = new TenantUserRegisterRequest(
                    invite.getCode(), OWNER_USERNAME, "dupuser@test.com", "Invited@1234");

            mockMvc.perform(post(REGISTER_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }
}