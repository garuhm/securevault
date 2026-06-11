package com.roadmap.securevault.platform.controller;

import com.roadmap.securevault.common.annotation.ApiVersion;
import com.roadmap.securevault.platform.dto.PlatformUserFilter;
import com.roadmap.securevault.platform.dto.PlatformUserResponse;
import com.roadmap.securevault.platform.dto.PlatformUserUpdateRequest;
import com.roadmap.securevault.platform.service.PlatformUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor

@RestController @ApiVersion("v1") @RequestMapping("/platform/users")
public class PlatformUserController {

    private final PlatformUserService platformUserService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PLATFORM_OWNER', 'PLATFORM_ADMIN', 'PLATFORM_SUPPORT')")
    public ResponseEntity<Page<PlatformUserResponse>> getUsers(
            @ParameterObject PlatformUserFilter filter,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(platformUserService.getUsers(filter, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PLATFORM_OWNER', 'PLATFORM_ADMIN', 'PLATFORM_SUPPORT')")
    public ResponseEntity<PlatformUserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(platformUserService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityEvaluator.isSelf(authentication, #id) " +
                   "or @securityEvaluator.isAbove(authentication, #id, T(com.roadmap.securevault.platform.entity.enums.PlatformRole))")
    public ResponseEntity<PlatformUserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody PlatformUserUpdateRequest request) {
        return ResponseEntity.ok(platformUserService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securityEvaluator.isAbove(authentication, #id, T(com.roadmap.securevault.platform.entity.enums.PlatformRole))")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        platformUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
