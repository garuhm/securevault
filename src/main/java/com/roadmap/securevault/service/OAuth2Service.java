package com.roadmap.securevault.service;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.OAuth2Properties;
import com.roadmap.securevault.entity.OAuth2Link;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.exception.OAuth2AuthenticationLinkException;
import com.roadmap.securevault.exception.OAuth2AuthenticationUnlinkException;
import com.roadmap.securevault.exception.OAuth2CredentialsExtractionException;
import com.roadmap.securevault.exception.OAuth2ProviderNotFoundException;
import com.roadmap.securevault.repo.OAuth2LinkRepository;
import com.roadmap.securevault.repo.UserRepository;
import com.roadmap.securevault.security.CustomOAuth2User;
import com.roadmap.securevault.security.PendingOAuth2User;
import com.roadmap.securevault.service.helper.CookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class OAuth2Service implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final OAuth2LinkRepository oAuth2LinkRepository;

    private final UserRepository userRepository;

    private final OAuth2Properties oauth2Properties;
    private final CookieProperties cookieProperties;

    private final CookieService cookieService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = delegate.loadUser(request);
        String provider = request.getClientRegistration().getRegistrationId();
        String providerUserId = extractProviderUserId(oAuth2User, provider);
        String email = extractEmail(oAuth2User, provider);

        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes())
                .getRequest();
        String linkingUsername = cookieService.getCookieValue(httpRequest, cookieProperties.oauth2LinkingRequestCookieName()).orElse(null);

        User user;
//        if linking cookie, then user wants new link
//        otherwise, only trying to log in
        if (linkingUsername != null) {
            user = createNewLink(linkingUsername, provider, providerUserId, email);
        } else {
            // normal login flow
            user = handleOAuth2Login(provider, providerUserId, email);
        }

        if(user == null) return new PendingOAuth2User(oAuth2User, email, provider, providerUserId);
        return new CustomOAuth2User(oAuth2User, user);
    }

    public void initiateLink(String provider, HttpServletResponse response) {
        if (!oauth2Properties.providerIdAttributes().containsKey(provider)) {
            throw new OAuth2ProviderNotFoundException("Unsupported provider: " + provider); // 404
        }

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        cookieService.addCookie(
                response,
                cookieProperties.oauth2LinkingRequestCookieName(),
                user.getUsername(),
                "/",
                cookieProperties.oauth2RequestCookieMaxAge());
    }

    @Transactional
    public void unlink(String provider) {
        if (!oauth2Properties.providerIdAttributes().containsKey(provider)) {
            throw new OAuth2ProviderNotFoundException("Unsupported provider: " + provider); // 404
        }

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!oAuth2LinkRepository.existsByProviderAndUserId(provider, user.getId())) {
            throw new OAuth2ProviderNotFoundException("Link with provider " + provider + " not found"); // 404
        }

        boolean hasPassword = user.getPassword() != null;
        boolean hasOtherLinks = oAuth2LinkRepository.countByUserId(user.getId()) > 1;

        if (!hasPassword && !hasOtherLinks) {
            throw new OAuth2AuthenticationUnlinkException(
                    "This OAuth2 link is the only one for this user, cannot unlink" // 400
            );
        }

        oAuth2LinkRepository.deleteByProviderAndUserId(provider, user.getId());
    }

    // links for existing accounts and completed pending registrations
    private User createNewLink(String linkingUsername, String provider,
                                     String providerUserId, String email) {
        User user = userRepository.findByUsername(linkingUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Platform user not found")); // 404

        // check this provider account isn't already linked to someone else
        if (oAuth2LinkRepository.existsByProviderAndProviderUserId(provider, providerUserId)) {
            throw new OAuth2AuthenticationLinkException(
                    "This " + provider + " account is already linked to another user" // 409
            );
        }

        // check this user doesn't already have this provider linked
        if (oAuth2LinkRepository.existsByProviderAndUserId(provider, user.getId())) {
            throw new OAuth2AuthenticationLinkException(
                    "You already have a " + provider + " account linked" // 409
            );
        }

        oAuth2LinkRepository.save(OAuth2Link.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .build());
        return user;
    }

    // for pending regs
    public void createNewLink(User user,
                              String provider,
                              String providerUserId,
                              String email) {
        oAuth2LinkRepository.save(OAuth2Link.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .build());
    }

    // oauth2 login, new user and existing
    private User handleOAuth2Login(String provider, String providerUserId,
                                   String email) {
        // if user already exists on our end, return user obj
        // else, return null bc registration needs to occur
        return oAuth2LinkRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(OAuth2Link::getUser)
                .orElseGet(() -> {
                    // if user with specific provider and provider user id does not exist yet
                    // but the email on the provider account is already in use by an account
                    // in this app, throw error
                    if(userRepository.existsByEmail(email)) {
                        throw new OAuth2AuthenticationLinkException(
                                "The email given by the provider account is already in use");
                    }
                    // else return null to signal new registration
                    return null;
                });
    }

    private String extractProviderUserId(OAuth2User oAuth2User, String provider) {
        String idAttribute = oauth2Properties.providerIdAttributes()
                .getOrDefault(provider, "sub");
        String id = oAuth2User.getAttribute(idAttribute);
        if (id == null) {
            throw new OAuth2CredentialsExtractionException(
                    "Could not extract user ID from provider: " + provider // 502
            );
        }
        return id;
    }

    private String extractEmail(OAuth2User oAuth2User, String provider) {
        String attribute = oauth2Properties.providerEmailAttributes()
                .getOrDefault(provider, "email");
        String email = oAuth2User.getAttribute(attribute);
        if (email == null) {
            throw new OAuth2CredentialsExtractionException(
                    "Could not extract email from provider: " + provider // 502
            );
        }
        return email;
    }
}
