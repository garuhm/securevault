package com.roadmap.securevault.repo;

import com.roadmap.securevault.entity.OAuth2Link;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OAuth2LinkRepository extends JpaRepository<OAuth2Link, UUID> {
    boolean existsByProviderAndUserId(String provider, UUID userId);
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);

    List<OAuth2Link> findAllByUserId(UUID userId);
    Optional<OAuth2Link> findByProviderAndProviderUserId(String provider, String providerUserId);

    @Modifying
    @Transactional
    void deleteByProviderAndUserId(String provider, UUID userId);

    int countByUserId(UUID id);
}
