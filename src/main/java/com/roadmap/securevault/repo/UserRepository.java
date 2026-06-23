package com.roadmap.securevault.repo;

import com.roadmap.securevault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends
        JpaRepository<User, UUID>,
        JpaSpecificationExecutor<User>
{
    Optional<User> findByUsername(String username);

    void deleteAllByUsernameIn(Collection<String> usernames);
}
