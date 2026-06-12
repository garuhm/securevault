package com.roadmap.securevault.tenant.controller;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.common.util.ApiVersioningResolver;
import com.roadmap.securevault.multitenancy.TenantContext;
import com.roadmap.securevault.tenant.dto.tenant_user.TenantUserUpdateRequest;
import com.roadmap.securevault.tenant.entity.Tenant;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import com.roadmap.securevault.tenant.entity.enums.TenantStatus;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("TenantUserController Integration Tests")
class TenantUserControllerIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired TenantRepository tenantRepository;
    @Autowired TenantUserRepository tenantUserRepository;
    @Autowired TenantSchemaInitializer tenantSchemaInitializer;
    @Autowired CookieProperties cookieProperties;
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
    private static final String USERS_URL =
            ApiVersioningResolver.resolve(TenantUserController.class, "getUsers", "/t/{code}/users");
    private static final String USER_BY_ID_URL =
            ApiVersioningResolver.resolve(TenantUserController.class, "getUserById", "/t/{code}/users/{id}");
    private static final String UPDATE_USER_URL =
            ApiVersioningResolver.resolve(TenantUserController.class, "updateUser", "/t/{code}/users/{id}");
    private static final String UPDATE_ROLE_URL =
            ApiVersioningResolver.resolve(TenantUserController.class, "updateRole", "/t/{code}/users/{id}/role");
    private static final String DELETE_USER_URL =
            ApiVersioningResolver.resolve(TenantUserController.class, "deleteUser", "/t/{code}/users/{id}");

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

        TenantContext.setTenantId(tenant.getSchemaName());
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
        TenantContext.setTenantId(tenant.getSchemaName());
        try {
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

    MvcResult authenticatedPut(String url, MvcResult result, Object body, Object... uriVars) throws Exception {
        return mockMvc.perform(put(url, uriVars)
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

    UUID userId(String username) {
        TenantContext.setTenantId(tenant.getSchemaName());
        try {
            return tenantUserRepository.findByUsername(username).get().getId();
        } finally {
            TenantContext.clear();
        }
    }

    boolean userExists(String username) {
        TenantContext.setTenantId(tenant.getSchemaName());
        try {
            return tenantUserRepository.existsByUsername(username);
        } finally {
            TenantContext.clear();
        }
    }

    @Nested
    @DisplayName("GET /t/{code}/users")
    class GetUsers {

        @Test
        @DisplayName("Any tenant role → 200 + page")
        void ownerCanGetUsers() throws Exception {
            MvcResult result = login(OWNER_USERNAME, OWNER_PASSWORD);

            mockMvc.perform(get(USERS_URL, COMPANY_CODE)
                            .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Member → 200 + page")
        void memberCanGetUsers() throws Exception {
            MvcResult result = login(MEMBER_USERNAME, MEMBER_PASSWORD);

            mockMvc.perform(get(USERS_URL, COMPANY_CODE)
                            .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedCannotGetUsers() throws Exception {
            mockMvc.perform(get(USERS_URL, COMPANY_CODE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Filter by role → 200 filtered results")
        void filterByRole() throws Exception {
            MvcResult result = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result2 = mockMvc.perform(get(USERS_URL, COMPANY_CODE)
                            .queryParam("role", "TENANT_ADMIN")
                            .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                    .andExpect(status().isOk())
                    .andReturn();

            String body = result2.getResponse().getContentAsString();
            assertThat(body).contains("TENANT_ADMIN");
            assertThat(body).doesNotContain("TENANT_OWNER");
            assertThat(body).doesNotContain("TENANT_MEMBER");
        }
    }

    @Nested
    @DisplayName("GET /t/{code}/users/{id}")
    class GetUserById {

        @Test
        @DisplayName("Valid id → 200")
        void validIdReturnsUser() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
            UUID adminId = userId(ADMIN_USERNAME);

            MvcResult result = authenticatedGet(USER_BY_ID_URL, loginResult, COMPANY_CODE, adminId);

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(result.getResponse().getContentAsString()).contains(ADMIN_USERNAME);
        }

        @Test
        @DisplayName("Non-existent id → 404")
        void nonExistentIdReturns404() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

            MvcResult result = authenticatedGet(USER_BY_ID_URL, loginResult, COMPANY_CODE, UUID.randomUUID());
            assertThat(result.getResponse().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedReturns401() throws Exception {
            UUID adminId = userId(ADMIN_USERNAME);

            mockMvc.perform(get(USER_BY_ID_URL, COMPANY_CODE, adminId))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /t/{code}/users/{id}")
    class UpdateUser {

        @Test
        @DisplayName("Self update → 200")
        void selfUpdate() throws Exception {
            MvcResult loginResult = login(MEMBER_USERNAME, MEMBER_PASSWORD);
            UUID memberId = userId(MEMBER_USERNAME);

            TenantUserUpdateRequest request = new TenantUserUpdateRequest("newmembername", null, null);

            MvcResult result = authenticatedPut(UPDATE_USER_URL, loginResult, request, COMPANY_CODE, memberId);

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(userExists("newmembername")).isTrue();
        }

        @Test
        @DisplayName("Admin updates member → 200")
        void adminUpdatesMember() throws Exception {
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);
            UUID memberId = userId(MEMBER_USERNAME);

            TenantUserUpdateRequest request = new TenantUserUpdateRequest("updatedmember", null, null);

            MvcResult result = authenticatedPut(UPDATE_USER_URL, loginResult, request, COMPANY_CODE, memberId);

            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(userExists("updatedmember")).isTrue();
        }

        @Test
        @DisplayName("Member updates other member → 403")
        void memberCannotUpdateOtherMember() throws Exception {
            TenantContext.setTenantId(tenant.getSchemaName());
            try {
                tenantUserRepository.saveAndFlush(TenantUser.builder()
                        .username("member2")
                        .email("member2@" + COMPANY_CODE + ".com")
                        .password(passwordEncoder.encode("Member2@1234"))
                        .role(TenantRole.TENANT_MEMBER)
                        .build());
            } finally {
                TenantContext.clear();
            }

            MvcResult loginResult = login(MEMBER_USERNAME, MEMBER_PASSWORD);
            UUID member2Id = userId("member2");

            TenantUserUpdateRequest request = new TenantUserUpdateRequest("hacked", null, null);

            MvcResult result = authenticatedPut(UPDATE_USER_URL, loginResult, request, COMPANY_CODE, member2Id);

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(userExists("hacked")).isFalse();
        }

        @Test
        @DisplayName("Duplicate username → 409")
        void duplicateUsernameReturns409() throws Exception {
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);
            UUID memberId = userId(MEMBER_USERNAME);

            TenantUserUpdateRequest request = new TenantUserUpdateRequest(OWNER_USERNAME, null, null);

            MvcResult result = authenticatedPut(UPDATE_USER_URL, loginResult, request, COMPANY_CODE, memberId);

            assertThat(result.getResponse().getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedReturns401() throws Exception {
            UUID memberId = userId(MEMBER_USERNAME);

            mockMvc.perform(put(UPDATE_USER_URL, COMPANY_CODE, memberId)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new TenantUserUpdateRequest("newname", null, null))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /t/{code}/users/{id}/role")
    class UpdateRole {

        @Test
        @DisplayName("Owner updates role → 200")
        void ownerUpdatesRole() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
            UUID memberId = userId(MEMBER_USERNAME);

            MvcResult result = mockMvc.perform(put(UPDATE_ROLE_URL, COMPANY_CODE, memberId)
                            .queryParam("role", "TENANT_ADMIN")
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName())))
                    .andReturn();

            assertThat(result.getResponse().getStatus()).isEqualTo(200);

            TenantContext.setTenantId(tenant.getSchemaName());
            try {
                assertThat(tenantUserRepository.findById(memberId).get().getRole())
                        .isEqualTo(TenantRole.TENANT_ADMIN);
            } finally {
                TenantContext.clear();
            }
        }

        @Test
        @DisplayName("Admin updates role → 403")
        void adminCannotUpdateRole() throws Exception {
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);
            UUID memberId = userId(MEMBER_USERNAME);

            MvcResult result = mockMvc.perform(put(UPDATE_ROLE_URL, COMPANY_CODE, memberId)
                            .queryParam("role", "TENANT_ADMIN")
                            .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName())))
                    .andReturn();

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedReturns401() throws Exception {
            UUID memberId = userId(MEMBER_USERNAME);

            mockMvc.perform(put(UPDATE_ROLE_URL, COMPANY_CODE, memberId)
                            .queryParam("role", "TENANT_ADMIN"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /t/{code}/users/{id}")
    class DeleteUser {

        @Test
        @DisplayName("Owner deletes member → 204")
        void ownerDeletesMember() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
            UUID memberId = userId(MEMBER_USERNAME);

            MvcResult result = authenticatedDelete(DELETE_USER_URL, loginResult, COMPANY_CODE, memberId);

            assertThat(result.getResponse().getStatus()).isEqualTo(204);
            assertThat(userExists(MEMBER_USERNAME)).isFalse();
        }

        @Test
        @DisplayName("Admin deletes member → 204")
        void adminDeletesMember() throws Exception {
            MvcResult loginResult = login(ADMIN_USERNAME, ADMIN_PASSWORD);
            UUID memberId = userId(MEMBER_USERNAME);

            MvcResult result = authenticatedDelete(DELETE_USER_URL, loginResult, COMPANY_CODE, memberId);

            assertThat(result.getResponse().getStatus()).isEqualTo(204);
            assertThat(userExists(MEMBER_USERNAME)).isFalse();
        }

        @Test
        @DisplayName("Delete owner → 403")
        void cannotDeleteOwner() throws Exception {
            MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
            UUID ownerId = userId(OWNER_USERNAME);

            MvcResult result = authenticatedDelete(DELETE_USER_URL, loginResult, COMPANY_CODE, ownerId);

            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(userExists(OWNER_USERNAME)).isTrue();
        }

        @Test
        @DisplayName("Unauthenticated → 401")
        void unauthenticatedReturns401() throws Exception {
            UUID memberId = userId(MEMBER_USERNAME);

            mockMvc.perform(delete(DELETE_USER_URL, COMPANY_CODE, memberId))
                    .andExpect(status().isUnauthorized());
        }
    }
}