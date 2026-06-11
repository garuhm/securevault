package com.roadmap.securevault.platform.service;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.exception.CredentialsTakenException;
import com.roadmap.securevault.common.service.AccessJwtService;
import com.roadmap.securevault.common.service.BaseAuthService;
import com.roadmap.securevault.common.service.CookieService;
import com.roadmap.securevault.platform.dto.PlatformUserRegisterRequest;
import com.roadmap.securevault.platform.entity.PlatformUser;
import com.roadmap.securevault.platform.entity.enums.PlatformRole;
import com.roadmap.securevault.platform.mapper.PlatformUserMapper;
import com.roadmap.securevault.platform.repo.PlatformUserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PlatformAuthService extends BaseAuthService<PlatformUser, PlatformUserRepository> {

    public PlatformAuthService(PlatformUserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               PlatformUserService userService,
                               AccessJwtService accessJwtService,
                               PlatformRefreshJwtService baseRefreshJwtService,
                               CookieService cookieService,
                               CookieProperties cookieProperties) {
        super(userRepository, passwordEncoder, userService,
                accessJwtService, baseRefreshJwtService, cookieService, cookieProperties);
    }

    @Transactional
    public void register(PlatformUserRegisterRequest credentials,
                         PlatformRole role) {
        if (userRepository.existsByUsername(credentials.username())) {
            throw new CredentialsTakenException("Username already exists");
        }
        if (userRepository.existsByEmail(credentials.email())) {
            throw new CredentialsTakenException("Email already exists");
        }

        PlatformUser user = PlatformUserMapper.toEntity(credentials);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(role);

        userRepository.save(user);
    }
}