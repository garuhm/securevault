package com.roadmap.securevault.test_util.testcontainers;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("testcontainers")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractTestContainersUtilizingTest {

    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("postgres:18").asCompatibleSubstituteFor("postgres");

    @Container
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

    @Container
    protected static final KeycloakContainer KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.3")
            .withRealmImportFile("app-realm-realm.json");

    @Container
    protected static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer("confluentinc/cp-kafka:7.7.0");

    @Container
    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        String issuerUri = KEYCLOAK.getAuthServerUrl() + "/realms/app-realm";
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuerUri);
        registry.add("security.keycloak.realm", () -> "app-realm");
        registry.add("security.keycloak.auth-server-url", KEYCLOAK::getAuthServerUrl);
        registry.add("security.keycloak.service-client-id", () -> "backend-client");
        registry.add("security.keycloak.service-client-secret", () -> fetchClientSecret("backend-client"));

        registry.add("test.keycloak.client-id", () -> "test-client");
        registry.add("test.keycloak.client-secret", () -> fetchClientSecret("test-client"));

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    private static String fetchClientSecret(String clientId) {
        String adminToken = RestClient.create().post()
                .uri(KEYCLOAK.getAuthServerUrl() + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=password&client_id=admin-cli&username="
                        + KEYCLOAK.getAdminUsername() + "&password=" + KEYCLOAK.getAdminPassword())
                .retrieve()
                .body(Map.class)
                .get("access_token")
                .toString();

        List<Map<String, Object>> clients = RestClient.create().get()
                .uri(KEYCLOAK.getAuthServerUrl() + "/admin/realms/app-realm/clients?clientId=" + clientId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(List.class);

        String clientUuid = (String) clients.get(0).get("id");

        Map<String, Object> secretResponse = RestClient.create().get()
                .uri(KEYCLOAK.getAuthServerUrl() + "/admin/realms/app-realm/clients/" + clientUuid + "/client-secret")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(Map.class);

        return (String) secretResponse.get("value");
    }
}