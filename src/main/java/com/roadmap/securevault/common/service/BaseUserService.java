package com.roadmap.securevault.common.service;

import com.roadmap.securevault.common.entity.BaseUser;
import com.roadmap.securevault.common.repo.BaseUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public abstract class BaseUserService<U extends BaseUser & UserDetails, R extends BaseUserRepository<U>>
        implements UserDetailsService {
    protected final R userRepository;

    protected BaseUserService(R userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username or password is incorrect"));
    }
}
