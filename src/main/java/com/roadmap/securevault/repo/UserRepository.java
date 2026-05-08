package com.roadmap.securevault.repo;

import com.roadmap.securevault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

//    List<User> findByRoles_Name(String roleName);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
