package com.roadmap.securevault.repo;

import com.roadmap.securevault.entity.OAuth2Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OAuth2LinkRepository extends JpaRepository<OAuth2Link, UUID> {
    List<OAuth2Link> findAllByUserId(UUID userId);
    Optional<OAuth2Link> findByProviderAndUserId(String provider, UUID userId);
}
