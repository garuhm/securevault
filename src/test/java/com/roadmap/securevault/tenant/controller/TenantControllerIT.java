package com.roadmap.securevault.tenant.controller;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.common.util.ApiVersioningResolver;
import com.roadmap.securevault.platform.entity.PlatformUser;
import com.roadmap.securevault.platform.entity.enums.PlatformRole;
import com.roadmap.securevault.platform.repo.PlatformRefreshTokenRepository;
import com.roadmap.securevault.platform.repo.PlatformUserRepository;
import com.roadmap.securevault.platform.controller.PlatformAuthController;
import com.roadmap.securevault.tenant.dto.tenant.TenantRegistrationRequest;
import com.roadmap.securevault.tenant.entity.Tenant;
import com.roadmap.securevault.tenant.entity.enums.TenantStatus;
import com.roadmap.securevault.tenant.repo.TenantRepository;
import com.roadmap.securevault.test_util.testcontainers.AbstractIT;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("TenantController Integration Tests")
class TenantControllerIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired PlatformUserRepository platformUserRepository;
    @Autowired PlatformRefreshTokenRepository platformRefreshTokenRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired CookieProperties cookieProperties;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String OWNER_USERNAME = "owner";
    private static final String OWNER_PASSWORD = "Owner@1234";
    private static final String SUPPORT_USERNAME = "support";
    private static final String SUPPORT_PASSWORD = "Support@1234";

    private static final String LOGIN_URL =
            ApiVersioningResolver.resolve(PlatformAuthController.class, "login", "/platform/auth/login");
    private static final String REGISTER_TENANT_URL =
            ApiVersioningResolver.resolve(TenantController.class, "registerTenant", "/platform/tenants/register");
    private static final String GET_TENANTS_URL =
            ApiVersioningResolver.resolve(TenantController.class, "getTenants", "/platform/tenants");
    private static final String GET_TENANT_BY_ID_URL =
            ApiVersioningResolver.resolve(TenantController.class, "getTenantById", "/platform/tenants/{id}");
    private static final String APPROVE_TENANT_URL =
            ApiVersioningResolver.resolve(TenantController.class, "approveTenant", "/platform/tenants/{id}/approve");
    private static final String SUSPEND_TENANT_URL =
            ApiVersioningResolver.resolve(TenantController.class, "suspendTenant", "/platform/tenants/{id}/suspend");
    private static final String UNSUSPEND_TENANT_URL =
            ApiVersioningResolver.resolve(TenantController.class, "unsuspendTenant", "/platform/tenants/{id}/unsuspend");

    @BeforeEach
    void setUp() {
        seedPlatformUsers();
    }

    @AfterEach
    void tearDown() {
        platformRefreshTokenRepository.deleteAll();
        tenantRepository.deleteAll();
        platformUserRepository.deleteAll();
    }

    void seedPlatformUsers() {
        if (!platformUserRepository.existsByUsername(OWNER_USERNAME)) {
            platformUserRepository.saveAndFlush(PlatformUser.builder()
                    .username(OWNER_USERNAME)
                    .email("owner@securevault.com")
                    .password(passwordEncoder.encode(OWNER_PASSWORD))
                    .role(PlatformRole.PLATFORM_OWNER)
                    .build());
        }
        if (!platformUserRepository.existsByUsername(SUPPORT_USERNAME)) {
            platformUserRepository.saveAndFlush(PlatformUser.builder()
                    .username(SUPPORT_USERNAME)
                    .email("support@securevault.com")
                    .password(passwordEncoder.encode(SUPPORT_PASSWORD))
                    .role(PlatformRole.PLATFORM_SUPPORT)
                    .build());
        }
    }

    MvcResult login(String username, String password) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
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

    MvcResult authenticatedGet(String url, MvcResult loginResult, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)
                        .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName())))
                .andReturn();
    }

    MvcResult authenticatedPost(String url, MvcResult loginResult, Object... uriVars) throws Exception {
        return mockMvc.perform(post(url, uriVars)
                        .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName())))
                .andReturn();
    }

    Tenant seedPendingTenant(String companyCode) {
        Tenant tenant = Tenant.builder()
                .companyName("Test Company " + companyCode)
                .companyCode(companyCode)
                .ownerEmail("owner@" + companyCode + ".com")
                .build();
        Tenant saved = tenantRepository.saveAndFlush(tenant);
        saved.setSchemaName("tenant_" + saved.getId().toString().replace("-", ""));
        return tenantRepository.saveAndFlush(saved);
    }

    Tenant seedApprovedTenant(String companyCode) {
        Tenant tenant = Tenant.builder()
                .companyName("Test Company " + companyCode)
                .companyCode(companyCode)
                .schemaName("tenant_" + companyCode)
                .ownerEmail("owner@" + companyCode + ".com")
                .build();
        tenant.setStatus(TenantStatus.APPROVED);
        return tenantRepository.saveAndFlush(tenant);
    }

    Tenant seedSuspendedTenant(String companyCode) {
        Tenant tenant = Tenant.builder()
                .companyName("Test Company " + companyCode)
                .companyCode(companyCode)
                .schemaName("tenant_" + companyCode)
                .ownerEmail("owner@" + companyCode + ".com")
                .build();
        tenant.setStatus(TenantStatus.SUSPENDED);
        return tenantRepository.saveAndFlush(tenant);
    }

    @Nested
    @DisplayName("POST /platform/tenants/register")
    class RegisterTenant {

        @Test
        @DisplayName("Valid request → 201")
        void validRequest() throws Exception {
            TenantRegistrationRequest request = new TenantRegistrationRequest(
                    "Acme Corp", "acme", "acme@acme.com");

            mockMvc.perform(post(REGISTER_TENANT_URL)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            assertThat(tenantRepository.existsByCompanyCode("acme")).isTrue();
            assertThat(tenantRepository.findByCompanyCode("acme").get().getStatus())
                    .isEqualTo(TenantStatus.PENDING);
        }

        @Test
        @DisplayName("Duplicate company code → 409")
        void duplicateCompanyCode() throws Exception {
            seedPendingTenant("acme");

            TenantRegistrationRequest request = new TenantRegistrationRequest(
                    "Acme Corp 2", "acme", "acme2@acme.com");

            mockMvc.perform(post(REGISTER_TENANT_URL)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());

            assertThat(tenantRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("Invalid fields → 400")
        void invalidFields() throws Exception {
            TenantRegistrationRequest request = new TenantRegistrationRequest(
                    "", "ac", "notanemail");

            mockMvc.perform(post(REGISTER_TENANT_URL)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/platform/tenants")
    class GetTenants {

        @Test
        @DisplayName("Any platform role → 200 + page")
        void ownerCanGetTenants() throws Exception {
            seedPendingTenant("acme");
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedGet(GET_TENANTS_URL, loginResult);

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(result.getResponse().getContentAsString()).contains("acme");
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedCannotGetTenants() throws Exception {
            mockMvc.perform(get(GET_TENANTS_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Filter by status → 200 filtered")
        void filterByStatus() throws Exception {
            seedPendingTenant("pending-co");
            seedApprovedTenant("approved-co");
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = mockMvc.perform(get(GET_TENANTS_URL)
                            .queryParam("status", "PENDING")
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName())))
                    .andExpect(status().isOk())
                    .andReturn();

            String body = result.getResponse().getContentAsString();
            assertThat(body).contains("pending-co");
            assertThat(body).doesNotContain("approved-co");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/platform/tenants/{id}")
    class GetTenantById {

        @Test
        @DisplayName("Valid id → 200")
        void validId() throws Exception {
            Tenant tenant = seedPendingTenant("acme");
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedGet(GET_TENANT_BY_ID_URL, loginResult, tenant.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(result.getResponse().getContentAsString()).contains("acme");
        }

        @Test
        @DisplayName("Non-existent id → 404")
        void nonExistentId() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedGet(GET_TENANT_BY_ID_URL, loginResult, UUID.randomUUID());

            assertThat(result.getResponse().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticated() throws Exception {
            Tenant tenant = seedPendingTenant("acme");

            mockMvc.perform(get(GET_TENANT_BY_ID_URL, tenant.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/platform/tenants/{id}/approve")
    class ApproveTenant {

        @Test
        @DisplayName("Pending tenant → 200 + bootstrap token")
        void approvePendingTenant() throws Exception {
            Tenant tenant = seedPendingTenant("acme");
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedPost(APPROVE_TENANT_URL, loginResult, tenant.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(result.getResponse().getContentAsString()).contains("bootstrapToken");
            assertThat(tenantRepository.findById(tenant.getId()).get().getStatus())
                    .isEqualTo(TenantStatus.APPROVED);
        }

        @Test
        @DisplayName("Nonexistent tenant → 404")
        void approveNonexistentTenant() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedPost(APPROVE_TENANT_URL, loginResult, UUID.randomUUID());

            assertThat(result.getResponse().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("Already approved → 409")
        void approveAlreadyApproved() throws Exception {
            Tenant tenant = seedApprovedTenant("acme");
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedPost(APPROVE_TENANT_URL, loginResult, tenant.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticated() throws Exception {
            Tenant tenant = seedPendingTenant("acme");

            mockMvc.perform(post(APPROVE_TENANT_URL, tenant.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Support role → 403")
        void supportCannotApprove() throws Exception {
            Tenant tenant = seedPendingTenant("acme");
            MvcResult loginResult = login(SUPPORT_USERNAME, SUPPORT_PASSWORD);

            MvcResult result = authenticatedPost(APPROVE_TENANT_URL, loginResult, tenant.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(tenantRepository.findById(tenant.getId()).get().getStatus())
                    .isEqualTo(TenantStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/platform/tenants/{id}/suspend")
    class SuspendTenant {

        @Test
        @DisplayName("Approved tenant → 200")
        void suspendApprovedTenant() throws Exception {
            Tenant tenant = seedApprovedTenant("acme");
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedPost(SUSPEND_TENANT_URL, loginResult, tenant.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(tenantRepository.findById(tenant.getId()).get().getStatus())
                    .isEqualTo(TenantStatus.SUSPENDED);
        }

        @Test
        @DisplayName("Nonexistent tenant → 404")
        void suspendNonexistentTenant() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedPost(SUSPEND_TENANT_URL, loginResult, UUID.randomUUID());

            assertThat(result.getResponse().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("Already suspended → 409")
        void suspendAlreadySuspended() throws Exception {
            Tenant tenant = seedSuspendedTenant("acme");
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedPost(SUSPEND_TENANT_URL, loginResult, tenant.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticated() throws Exception {
            Tenant tenant = seedApprovedTenant("acme");

            mockMvc.perform(post(SUSPEND_TENANT_URL, tenant.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Support role → 403")
        void supportCannotSuspend() throws Exception {
            Tenant tenant = seedApprovedTenant("acme");
            MvcResult loginResult = login(SUPPORT_USERNAME, SUPPORT_PASSWORD);

            MvcResult result = authenticatedPost(SUSPEND_TENANT_URL, loginResult, tenant.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(tenantRepository.findById(tenant.getId()).get().getStatus())
                    .isEqualTo(TenantStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/platform/tenants/{id}/unsuspend")
    class UnsuspendTenant {

        @Test
        @DisplayName("Suspended tenant → 200")
        void unsuspendSuspendedTenant() throws Exception {
            Tenant tenant = seedSuspendedTenant("acme");
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedPost(UNSUSPEND_TENANT_URL, loginResult, tenant.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(tenantRepository.findById(tenant.getId()).get().getStatus())
                    .isEqualTo(TenantStatus.APPROVED);
        }

        @Test
        @DisplayName("Nonexistent tenant → 404")
        void unsuspendNonexistentTenant() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedPost(UNSUSPEND_TENANT_URL, loginResult, UUID.randomUUID());

            assertThat(result.getResponse().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("Not suspended → 409")
        void unsuspendNotSuspended() throws Exception {
            Tenant tenant = seedApprovedTenant("acme");
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedPost(UNSUSPEND_TENANT_URL, loginResult, tenant.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticated() throws Exception {
            Tenant tenant = seedSuspendedTenant("acme");

            mockMvc.perform(post(UNSUSPEND_TENANT_URL, tenant.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Support role → 403")
        void supportCannotUnsuspend() throws Exception {
            Tenant tenant = seedSuspendedTenant("acme");
            MvcResult loginResult = login(SUPPORT_USERNAME, SUPPORT_PASSWORD);

            MvcResult result = authenticatedPost(UNSUSPEND_TENANT_URL, loginResult, tenant.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(tenantRepository.findById(tenant.getId()).get().getStatus())
                    .isEqualTo(TenantStatus.SUSPENDED);
        }
    }
}