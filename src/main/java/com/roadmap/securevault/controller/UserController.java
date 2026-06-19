package com.roadmap.securevault.controller;

import com.roadmap.securevault.annotation.ApiVersion;
import com.roadmap.securevault.dto.keycloak.KeycloakUserRepresentation;
import com.roadmap.securevault.dto.web.UserUpdateRequest;
import com.roadmap.securevault.service.web.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@ApiVersion("v1")
@RequestMapping("/users")

@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PutMapping("/{userId}")
    @PreAuthorize("@securityEvaluator.isSelf(authentication, #id) " +
                   "or @securityEvaluator.isAbove(authentication, #id) ")
    public ResponseEntity<KeycloakUserRepresentation> updateUser(
            @PathVariable UUID userId,
            @RequestBody @Validated(UserUpdateRequest.Full.class) UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("@securityEvaluator.isSelf(authentication, #id) " +
                   "or @securityEvaluator.isAbove(authentication, #id) ")
    public ResponseEntity<KeycloakUserRepresentation> patchUser(
            @PathVariable UUID userId,
            @RequestBody @Validated(UserUpdateRequest.Partial.class) UserUpdateRequest request) {
        return ResponseEntity.ok(userService.partiallyUpdateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@securityEvaluator.isSelf(authentication, #id) " +
                   "or @securityEvaluator.isAbove(authentication, #id) ")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
