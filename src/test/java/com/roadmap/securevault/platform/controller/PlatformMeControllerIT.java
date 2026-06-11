package com.roadmap.securevault.platform.controller;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.dto.LoginRequest;
import com.roadmap.securevault.common.util.ApiVersioningResolver;
import com.roadmap.securevault.test_util.testcontainers.AbstractIT;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("PlatformMeController Integration Tests")
public class PlatformMeControllerIT extends AbstractIT {
    @Autowired MockMvc mockMvc;
    @Autowired CookieProperties cookieProperties;
    @Autowired ObjectMapper objectMapper;

    private static final String OWNER_USERNAME = "owner";
    private static final String OWNER_EMAIL = "owner@securevault.com";
    private static final String OWNER_PASSWORD = "Owner@1234";

    private static final String LOGIN_URL =
            ApiVersioningResolver.resolve(PlatformAuthController.class, "login", "/platform/auth/login");
    private static final String ME_URL =
            ApiVersioningResolver.resolve(PlatformMeController.class, "getMe", "/platform/me");

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

    MvcResult authenticatedGet(String url, MvcResult result, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)
                        .cookie(extractCookie(result, cookieProperties.accessTokenCookieName())))
                .andReturn();
    }

    @Test
    @DisplayName("Authenticated → 200")
    void authenticatedCanGetMe() throws Exception {
        MvcResult loginResult = login(OWNER_USERNAME, OWNER_PASSWORD);
        MvcResult result = authenticatedGet(ME_URL, loginResult);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains(OWNER_USERNAME);
        assertThat(result.getResponse().getContentAsString()).contains(OWNER_EMAIL);
    }

    @Test
    @DisplayName("Unauthenticated → 401")
    void unauthenticatedCannotGetMe() throws Exception {
        MvcResult result = mockMvc.perform(get(ME_URL))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }
}
