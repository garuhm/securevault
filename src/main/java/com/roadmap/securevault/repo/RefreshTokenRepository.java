package com.roadmap.securevault.repo;

import com.roadmap.securevault.entity.RefreshToken;
import com.roadmap.securevault.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByIdAndRevokedFalse(UUID id);
    List<RefreshToken> findAllByUserAndRevokedFalse(User user);

    // Revoke all tokens for a user
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user = :user AND t.revoked = false")
    void revokeAllByUser(@Param("user") User user);

     // for cleanup
    @Modifying
    @Transactional
    void deleteByExpiryDateBefore(Date date);

    @Modifying
    @Transactional
    void deleteByRevokedTrue();
}
