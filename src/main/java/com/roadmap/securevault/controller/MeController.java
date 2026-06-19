package com.roadmap.securevault.controller;

import com.roadmap.securevault.annotation.ApiVersion;
import com.roadmap.securevault.dto.web.MeResponse;
import com.roadmap.securevault.dto.web.MeRolesBelowResponse;
import com.roadmap.securevault.service.web.MeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiVersion("v1")
@RequestMapping("/me")

@RequiredArgsConstructor
public class MeController {
    private final MeService meService;

    @GetMapping
    public ResponseEntity<MeResponse> getMe() {
        return ResponseEntity.ok(meService.getMe());
    }

    @GetMapping("/roles-below")
    public ResponseEntity<MeRolesBelowResponse> getMeRolesBelow() {
        return ResponseEntity.ok(meService.getRolesBelow());
    }
}