package com.roadmap.securevault.controller;

import com.roadmap.securevault.annotation.ApiVersion;
import com.roadmap.securevault.dto.web.MeResponse;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

@RequiredArgsConstructor

@ApiVersion("v1")
public class MeController {
    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe() {
        return ResponseEntity.ok(UserMapper.toMeResponse((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
    }
}