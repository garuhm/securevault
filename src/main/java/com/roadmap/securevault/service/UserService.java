package com.roadmap.securevault.service;

import com.roadmap.securevault.dto.MeResponse;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username or password is incorrect"));
    }

    public MeResponse me() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return new MeResponse(user.getUsername(), user.getEmail());
    }
}
