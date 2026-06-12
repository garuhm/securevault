package com.roadmap.securevault.tenant.controller;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.config.properties.RedisProperties;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.common.util.ApiVersioningResolver;
import com.roadmap.securevault.multitenancy.TenantContext;
import com.roadmap.securevault.tenant.dto.invite.InviteCreateRequest;
import com.roadmap.securevault.tenant.entity.InviteCode;
import com.roadmap.securevault.tenant.entity.Tenant;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import com.roadmap.securevault.tenant.entity.enums.TenantStatus;
import com.roadmap.securevault.tenant.repo.InviteCodeRepository;
import com.roadmap.securevault.tenant.repo.TenantRepository;
import com.roadmap.securevault.tenant.repo.TenantUserRepository;
import com.roadmap.securevault.tenant.service.TenantSchemaInitializer;
import com.roadmap.securevault.test_util.testcontainers.AbstractIT;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("InviteController Integration Tests")
class InviteControllerIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired TenantRepository tenantRepository;
    @Autowired TenantUserRepository tenantUserRepository;
    @Autowired InviteCodeRepository inviteCodeRepository;
    @Autowired TenantSchemaInitializer tenantSchemaInitializer;
    @Autowired CookieProperties cookieProperties;
    @Autowired RedisProperties redisProperties;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final String COMPANY_CODE = "acme";
    private static final String OWNER_USERNAME = "owner";
    private static final String OWNER_PASSWORD = "Owner@1234";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@1234";
    private static final String MEMBER_USERNAME = "member";
    private static final String MEMBER_PASSWORD = "Member@1234";

    private static final String LOGIN_URL =
            ApiVersioningResolver.resolve(TenantAuthController.class, "login", "/t/{code}/auth/login");
    private static final String CREATE_INVITE_URL =
            ApiVersioningResolver.resolve(InviteController.class, "createInvite", "/t/{code}/invites");
    private static final String GET_INVITES_URL =
            ApiVersioningResolver.resolve(InviteController.class, "getInvites", "/t/{code}/invites");
    private static final String GET_INVITE_BY_ID_URL =
            ApiVersioningResolver.resolve(InviteController.class, "getInviteById", "/t/{code}/invites/{id}");
    private static final String REVOKE_INVITE_URL =
            ApiVersioningResolver.resolve(InviteController.class, "revokeInvite", "/t/{code}/invites/{id}");

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

        TenantContext.setTenantSchema(tenant.getSchemaName());
        try {
            seedTenantUsers();
        } finally {
            TenantContext.clear();
        }
    }

    void seedTenantUsers() {
        tenantUserRepository.saveAndFlush(TenantUser.builder()
                .username(OWNER_USERNAME)
                .email("owner@" + COMPANY_CODE + ".com")
                .password(passwordEncoder.encode(OWNER_PASSWORD))
                .role(TenantRole.TENANT_OWNER)
                .build());
        tenantUserRepository.saveAndFlush(TenantUser.builder()
                .username(ADMIN_USERNAME)
                .email("admin@" + COMPANY_CODE + ".com")
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(TenantRole.TENANT_ADMIN)
                .build());
        tenantUserRepository.saveAndFlush(TenantUser.builder()
                .username(MEMBER_USERNAME)
                .email("member@" + COMPANY_CODE + ".com")
                .password(passwordEncoder.encode(MEMBER_PASSWORD))
                .role(TenantRole.TENANT_MEMBER)
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.setTenantSchema(tenant.getSchemaName());
        try {
            inviteCodeRepository.deleteAll();
            tenantUserRepository.deleteAll();
        } finally {
            TenantContext.clear();
        }
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

    MvcResult login(String username, String password) throws Exception {
        return mockMvc.perform(post(LOGIN_URL, COMPANY_CODE)
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

    MvcResult authenticatedGet(String url, MvcResult result, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)
                        .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                .andReturn();
    }

    MvcResult authenticatedPost(String url, MvcResult result, Object body, Object... uriVars) throws Exception {
        return mockMvc.perform(post(url, uriVars)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body))
                        .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                .andReturn();
    }

    MvcResult authenticatedDelete(String url, MvcResult result, Object... uriVars) throws Exception {
        return mockMvc.perform(delete(url, uriVars)
                        .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                .andReturn();
    }

    InviteCode seedInvite(String inviteeEmail, TenantRole role, String createdByUsername, boolean used) {
        TenantContext.setTenantSchema(tenant.getSchemaName());
        try {
            TenantUser creator = tenantUserRepository.findByUsername(createdByUsername).get();

            InviteCode invite = InviteCode.builder()
                    .createdBy(creator)
                    .inviteeEmail(inviteeEmail)
                    .role(role)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            if (used) {
                invite.setUsedAt(LocalDateTime.now());
            }

            InviteCode savedInvite = inviteCodeRepository.saveAndFlush(invite);
            savedInvite.setCode(redisProperties.inviteTokenPrefix() + savedInvite.getId().toString());
            return inviteCodeRepository.saveAndFlush(savedInvite);
        } finally {
            TenantContext.clear();
        }
    }

    InviteCode getInvite(UUID id) {
        TenantContext.setTenantSchema(tenant.getSchemaName());
        try {
            return inviteCodeRepository.findById(id).get();
        } finally {
            TenantContext.clear();
        }
    }

    int findAllInvites() {
        TenantContext.setTenantSchema(tenant.getSchemaName());
        try {
            return inviteCodeRepository.findAll().size();
        } finally {
            TenantContext.clear();
        }
    }

    @Nested
    @DisplayName("POST /t/{code}/invites")
    class CreateInvite {

        @Test
        @DisplayName("Owner creates → 201 + token in response")
        void ownerCreatesInvite() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            InviteCreateRequest request = new InviteCreateRequest("newuser@test.com", TenantRole.TENANT_MEMBER);

            MvcResult result = authenticatedPost(CREATE_INVITE_URL, loginResult, request, COMPANY_CODE);

            assertThat(result.getResponse().getStatus()).isEqualTo(201);
            assertThat(result.getResponse().getContentAsString()).contains("code");
            assertThat(findAllInvites()).isEqualTo(1);
        }

        @Test
        @DisplayName("Admin creates → 201")
        void adminCreatesInvite() throws Exception {
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);

            InviteCreateRequest request = new InviteCreateRequest("newuser2@test.com", TenantRole.TENANT_MEMBER);

            MvcResult result = authenticatedPost(CREATE_INVITE_URL, loginResult, request, COMPANY_CODE);

            assertThat(result.getResponse().getStatus()).isEqualTo(201);
            assertThat(findAllInvites()).isEqualTo(1);
        }

        @Test
        @DisplayName("Member creates → 403")
        void memberCannotCreateInvite() throws Exception {
            MvcResult loginResult = login(MEMBER_USERNAME, MEMBER_PASSWORD);

            InviteCreateRequest request = new InviteCreateRequest("newuser3@test.com", TenantRole.TENANT_MEMBER);

            MvcResult result = authenticatedPost(CREATE_INVITE_URL, loginResult, request, COMPANY_CODE);

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(findAllInvites()).isEqualTo(0);
        }

        @Test
        @DisplayName("Duplicate invitee email → 409")
        void duplicateInviteeEmail() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            InviteCreateRequest request = new InviteCreateRequest("dup@test.com", TenantRole.TENANT_MEMBER);

            authenticatedPost(CREATE_INVITE_URL, loginResult, request, COMPANY_CODE);
            MvcResult result = authenticatedPost(CREATE_INVITE_URL, loginResult, request, COMPANY_CODE);

            assertThat(result.getResponse().getStatus()).isEqualTo(409);
            assertThat(findAllInvites()).isEqualTo(1);
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedReturns401() throws Exception {
            InviteCreateRequest request = new InviteCreateRequest("newuser4@test.com", TenantRole.TENANT_MEMBER);

            mockMvc.perform(post(CREATE_INVITE_URL, COMPANY_CODE)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            assertThat(findAllInvites()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("GET /t/{code}/invites")
    class GetInvites {

        @Test
        @DisplayName("Owner → 200 + page")
        void ownerCanGetInvites() throws Exception {
            seedInvite("a@test.com", TenantRole.TENANT_MEMBER, OWNER_USERNAME, false);
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedGet(GET_INVITES_URL, loginResult, COMPANY_CODE);

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(result.getResponse().getContentAsString()).contains("a@test.com");
        }

        @Test
        @DisplayName("Admin → 200 + page")
        void adminCanGetInvites() throws Exception {
            seedInvite("b@test.com", TenantRole.TENANT_MEMBER, OWNER_USERNAME, false);
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);

            MvcResult result = authenticatedGet(GET_INVITES_URL, loginResult, COMPANY_CODE);

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Filter used=true → 200 only used")
        void filterUsedTrue() throws Exception {
            seedInvite("active@test.com", TenantRole.TENANT_MEMBER, OWNER_USERNAME, false);
            seedInvite("used@test.com", TenantRole.TENANT_MEMBER, OWNER_USERNAME, true);
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = mockMvc.perform(get(GET_INVITES_URL, COMPANY_CODE)
                            .queryParam("used", "true")
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName())))
                    .andReturn();

            String body = result.getResponse().getContentAsString();
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(body).contains("used@test.com");
            assertThat(body).doesNotContain("active@test.com");
        }

        @Test
        @DisplayName("Filter used=false → 200 only active")
        void filterUsedFalse() throws Exception {
            seedInvite("active2@test.com", TenantRole.TENANT_MEMBER, OWNER_USERNAME, false);
            seedInvite("used2@test.com", TenantRole.TENANT_MEMBER, OWNER_USERNAME, true);
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = mockMvc.perform(get(GET_INVITES_URL, COMPANY_CODE)
                            .queryParam("used", "false")
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName())))
                    .andReturn();

            String body = result.getResponse().getContentAsString();
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(body).contains("active2@test.com");
            assertThat(body).doesNotContain("used2@test.com");
        }

        @Test
        @DisplayName("Member → 403")
        void memberCannotGetInvites() throws Exception {
            MvcResult loginResult = login(MEMBER_USERNAME, MEMBER_PASSWORD);

            MvcResult result = authenticatedGet(GET_INVITES_URL, loginResult, COMPANY_CODE);

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedReturns401() throws Exception {
            mockMvc.perform(get(GET_INVITES_URL, COMPANY_CODE))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /t/{code}/invites/{id}")
    class GetInviteById {

        @Test
        @DisplayName("Owner → 200")
        void ownerCanGetInviteById() throws Exception {
            InviteCode invite = seedInvite("c@test.com", TenantRole.TENANT_MEMBER, ADMIN_USERNAME, false);
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedGet(GET_INVITE_BY_ID_URL, loginResult, COMPANY_CODE, invite.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Admin → 200")
        void adminCanGetInviteById() throws Exception {
            InviteCode invite = seedInvite("d@test.com", TenantRole.TENANT_MEMBER, OWNER_USERNAME, false);
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);

            MvcResult result = authenticatedGet(GET_INVITE_BY_ID_URL, loginResult, COMPANY_CODE, invite.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Creator (member) → 200")
        void creatorMemberCanGetOwnInvite() throws Exception {
            // promote member to admin temporarily isn't needed - members can't create invites,
            // so to test "creator" path for a member-created invite we need a member who created one.
            // Since members can't create via the endpoint, seed directly with member as creator.
            InviteCode invite = seedInvite("e@test.com", TenantRole.TENANT_MEMBER, MEMBER_USERNAME, false);
            MvcResult loginResult = login(MEMBER_USERNAME, MEMBER_PASSWORD);

            MvcResult result = authenticatedGet(GET_INVITE_BY_ID_URL, loginResult, COMPANY_CODE, invite.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Non-creator member → 403")
        void nonCreatorMemberCannotGetInvite() throws Exception {
            InviteCode invite = seedInvite("f@test.com", TenantRole.TENANT_MEMBER, ADMIN_USERNAME, false);
            MvcResult loginResult = login(MEMBER_USERNAME, MEMBER_PASSWORD);

            MvcResult result = authenticatedGet(GET_INVITE_BY_ID_URL, loginResult, COMPANY_CODE, invite.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("Non-existent → 404")
        void nonExistentReturns404() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedGet(GET_INVITE_BY_ID_URL, loginResult, COMPANY_CODE, UUID.randomUUID());

            assertThat(result.getResponse().getStatus()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("DELETE /t/{code}/invites/{id}")
    class RevokeInvite {

        @Test
        @DisplayName("Owner revokes → 204")
        void ownerRevokesInvite() throws Exception {
            InviteCode invite = seedInvite("g@test.com", TenantRole.TENANT_MEMBER, ADMIN_USERNAME, false);
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedDelete(REVOKE_INVITE_URL, loginResult, COMPANY_CODE, invite.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(204);
            assertThat(getInvite(invite.getId()).getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("Creator revokes own → 204")
        void creatorRevokesOwnInvite() throws Exception {
            InviteCode invite = seedInvite("h@test.com", TenantRole.TENANT_MEMBER, ADMIN_USERNAME, false);
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);

            MvcResult result = authenticatedDelete(REVOKE_INVITE_URL, loginResult, COMPANY_CODE, invite.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(204);
            assertThat(getInvite(invite.getId()).getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("Already used → 409")
        void alreadyUsedReturns409() throws Exception {
            InviteCode invite = seedInvite("i@test.com", TenantRole.TENANT_MEMBER, OWNER_USERNAME, true);
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedDelete(REVOKE_INVITE_URL, loginResult, COMPANY_CODE, invite.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("Non-creator member → 403")
        void nonCreatorMemberCannotRevoke() throws Exception {
            InviteCode invite = seedInvite("j@test.com", TenantRole.TENANT_MEMBER, ADMIN_USERNAME, false);
            MvcResult loginResult = login(MEMBER_USERNAME, MEMBER_PASSWORD);

            MvcResult result = authenticatedDelete(REVOKE_INVITE_URL, loginResult, COMPANY_CODE, invite.getId());

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(getInvite(invite.getId()).getUsedAt()).isNull();
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedReturns401() throws Exception {
            InviteCode invite = seedInvite("k@test.com", TenantRole.TENANT_MEMBER, OWNER_USERNAME, false);

            mockMvc.perform(delete(REVOKE_INVITE_URL, COMPANY_CODE, invite.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }
}