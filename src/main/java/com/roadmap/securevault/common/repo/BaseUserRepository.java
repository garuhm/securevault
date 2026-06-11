package com.roadmap.securevault.common.repo;

import com.roadmap.securevault.common.entity.BaseUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface BaseUserRepository<U extends BaseUser> extends JpaRepository<U, UUID> {
    Optional<U> findByUsername(String username);
    Optional<U> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}