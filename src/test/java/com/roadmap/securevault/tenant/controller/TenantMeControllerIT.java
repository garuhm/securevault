package com.roadmap.securevault.tenant.controller;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.common.util.ApiVersioningResolver;
import com.roadmap.securevault.multitenancy.TenantContext;
import com.roadmap.securevault.tenant.entity.Tenant;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import com.roadmap.securevault.tenant.entity.enums.TenantStatus;
import com.roadmap.securevault.tenant.repo.TenantRepository;
import com.roadmap.securevault.tenant.repo.TenantUserRepository;
import com.roadmap.securevault.tenant.service.TenantSchemaInitializer;
import com.roadmap.securevault.test_util.testcontainers.AbstractIT;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("TenantMeController Integration Tests")
class TenantMeControllerIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired TenantRepository tenantRepository;
    @Autowired TenantUserRepository tenantUserRepository;
    @Autowired TenantSchemaInitializer tenantSchemaInitializer;
    @Autowired CookieProperties cookieProperties;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final String COMPANY_CODE = "acme";
    private static final String OWNER_USERNAME = "tenantowner";
    private static final String OWNER_PASSWORD = "Owner@1234";

    private static final String LOGIN_URL =
            ApiVersioningResolver.resolve(TenantAuthController.class, "login", "/t/{code}/auth/login");
    private static final String ME_URL =
            ApiVersioningResolver.resolve(TenantMeController.class, "getMe", "/t/{code}/me");

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
            tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .username(OWNER_USERNAME)
                    .email("owner@" + COMPANY_CODE + ".com")
                    .password(passwordEncoder.encode(OWNER_PASSWORD))
                    .role(TenantRole.TENANT_OWNER)
                    .build());
        } finally {
            TenantContext.clear();
        }
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

    @Test
    @DisplayName("Authenticated → 200 + companyCode in response")
    void authenticatedCanGetMe() throws Exception {
        MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);

        MvcResult result = mockMvc.perform(get(ME_URL, COMPANY_CODE)
                        .cookie(extractCookie(loginResult, cookieProperties.accessTokenCookieName())))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains(OWNER_USERNAME);
        assertThat(result.getResponse().getContentAsString()).contains(COMPANY_CODE);
    }

    @Test
    @DisplayName("Unauthenticated → 401")
    void unauthenticatedCannotGetMe() throws Exception {
        MvcResult result = mockMvc.perform(get(ME_URL, COMPANY_CODE))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }
}