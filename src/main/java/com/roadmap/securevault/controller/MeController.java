package com.roadmap.securevault.controller;

import com.roadmap.securevault.controller.annotation.ApiVersion;
import com.roadmap.securevault.dto.MeResponse;
import com.roadmap.securevault.entity.Role;
import com.roadmap.securevault.entity.enums.RoleName;
import com.roadmap.securevault.repo.RoleRepository;
import com.roadmap.securevault.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

@RequiredArgsConstructor

@ApiVersion("v1")
public class MeController {
    private final UserService userService;
//    private final RoleRepository roleRepository;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe() {
        return ResponseEntity.ok(userService.me());
    }
//    @GetMapping("/me")
//    public void getMe2() {
//                roleRepository.saveAndFlush(Role.builder().name(RoleName.ROLE_USER).build());
//        roleRepository.saveAndFlush(Role.builder().name(RoleName.ROLE_ADMIN).build());
//    }
}
