package com.roadmap.securevault.common.service;

import com.roadmap.securevault.common.dto.UserResponse;
import com.roadmap.securevault.common.dto.UserUpdateRequest;
import com.roadmap.securevault.common.entity.BaseUser;
import com.roadmap.securevault.common.exception.CredentialsTakenException;
import com.roadmap.securevault.common.repo.BaseUserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

public abstract class BaseUserService <
        U extends BaseUser & UserDetails,
        R extends BaseUserRepository<U> & JpaSpecificationExecutor<U>,
        RESP extends UserResponse,
        REQ extends UserUpdateRequest>
        implements UserDetailsService {

    protected final R userRepository;
    protected final PasswordEncoder passwordEncoder;

    protected BaseUserService(R userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username or password is incorrect"));
    }

    protected abstract RESP toResponse(U user);
    protected abstract boolean isOwnerRole(U user);

    public Page<RESP> getUsers(Specification<U> spec, Pageable pageable) {
        return userRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    public RESP getUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional
    public RESP updateUser(UUID userId, REQ request) {
        U user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (request.username() != null) {
            if (userRepository.existsByUsername(request.username())) {
                throw new CredentialsTakenException("Username already exists");
            }
            user.setUsername(request.username());
        }
        if (request.email() != null) {
            if (userRepository.existsByEmail(request.email())) {
                throw new CredentialsTakenException("Email already exists");
            }
            user.setEmail(request.email());
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID userId) {
        U user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (isOwnerRole(user)) {
            throw new IllegalStateException("Owner account cannot be deleted");
        }

        userRepository.deleteById(userId);
    }
}