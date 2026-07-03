package com.roadmap.securevault.service.web;

import com.roadmap.securevault.config.KafkaTopics;
import com.roadmap.securevault.config.properties.KeycloakProperties;
import com.roadmap.securevault.config.properties.OAuth2Properties;
import com.roadmap.securevault.dto.user.RegisterRequest;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.exception.CredentialsTakenException;
import com.roadmap.securevault.exception.InvalidOAuth2ProviderException;
import com.roadmap.securevault.kafka.events.UserEvent;
import com.roadmap.securevault.service.helper.KeycloakAuthClient;
import com.roadmap.securevault.service.helper.PKCEGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final OAuth2Properties oAuth2Properties;
    private final KeycloakProperties keycloakProperties;
    private final KeycloakAuthClient keycloakAuthClient;
    private final PKCEGenerationService pkceGenerationService;
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public void register(RegisterRequest credentials) {
        if (keycloakAuthClient.userExistsByUsername(credentials.username())) {
            throw new CredentialsTakenException("Username already exists");
        }
        if (keycloakAuthClient.userExistsByEmail(credentials.email())) {
            throw new CredentialsTakenException("Email already exists");
        }

        UUID userId = keycloakAuthClient.createUser(credentials, RoleName.ROLE_USER);

        UserEvent event = new UserEvent(
                userId,
                credentials.username(),
                credentials.email(),
                UserEvent.UserEventType.CREATED,
                Set.of(RoleName.ROLE_USER)
        );
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.id().toString(), event);
    }

    public void logoutAllSessions() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        keycloakAuthClient.logoutAllSessions(user.getId());
    }

    public List<String> getOAuth2ProviderLink(String provider) {
        if(!oAuth2Properties.providers().contains(provider))
            throw new InvalidOAuth2ProviderException("Invalid OAuth2 provider");

        String codeVerifier = pkceGenerationService.generateCodeVerifier();
        String codeChallenge = pkceGenerationService.generateCodeChallenge(codeVerifier);

        String linkUrl = UriComponentsBuilder
            .fromUriString(keycloakProperties.idpLinkActionUri())
            .queryParam("client_id", oAuth2Properties.clientId())
            .queryParam("redirect_uri", oAuth2Properties.redirectUrl())
            .queryParam("response_type", "code")
            .queryParam("scope", "openid")
            .queryParam("kc_action", "idp_link:" + provider)
            .queryParam("code_challenge", codeChallenge)
            .queryParam("code_challenge_method", "S256")
            .build()
            .toUriString();

        ResponseCookie verifierCookie = ResponseCookie.from("pkce_verifier", codeVerifier)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .maxAge(Duration.ofMinutes(5))
            .path("/")
            .build();

        return List.of(linkUrl, verifierCookie.toString());
    }
}