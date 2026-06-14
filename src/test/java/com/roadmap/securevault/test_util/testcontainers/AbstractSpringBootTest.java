package com.roadmap.securevault.test_util.testcontainers;

import com.roadmap.securevault.config.properties.KeycloakProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Base Integration Test with a PostgreSQL Testcontainer **/
@SpringBootTest
public abstract class AbstractSpringBootTest extends AbstractTestContainersUtilizingTest {
    @Autowired
    private KeycloakProperties keycloakProperties;

    @Value("${test.keycloak.client-id}")
    private String testClientId;

    @Value("${test.keycloak.client-secret}")
    private String testClientSecret;


    /**
     * Test-only token acquisition via a dedicated test client with direct access
     * grants enabled in the test realm. This mints tokens for test fixtures only —
     * it's unrelated to how the application itself authenticates real users.
     */
    protected String obtainAccessToken(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", testClientId);
        form.add("client_secret", testClientSecret);
        form.add("username", username);
        form.add("password", password);

        Map<String, Object> response = RestClient.create()
                .post()
                .uri(keycloakProperties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        return (String) response.get("access_token");
    }
}

