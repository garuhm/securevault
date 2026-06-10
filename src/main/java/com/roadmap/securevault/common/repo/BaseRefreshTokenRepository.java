package com.roadmap.securevault.common.repo;

import com.roadmap.securevault.common.entity.BaseRefreshToken;
import com.roadmap.securevault.common.entity.BaseUser;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BaseRefreshTokenRepository<T extends BaseRefreshToken<U>, U extends BaseUser & UserDetails>
        extends JpaRepository<T, UUID> {

    Optional<T> findByIdAndRevokedFalse(UUID id);
    List<T> findAllByUserAndRevokedFalse(U user);

    @Modifying
    @Transactional
    void deleteByExpiryDateBefore(Date date);

    void revokeAllByUser(@Param("user") U user);

    @Modifying
    @Transactional
    void deleteByRevokedTrue();
}
