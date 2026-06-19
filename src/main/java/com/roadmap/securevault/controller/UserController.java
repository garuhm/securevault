package com.roadmap.securevault.controller;

import com.roadmap.securevault.annotation.ApiVersion;
import com.roadmap.securevault.dto.keycloak.KeycloakUserRepresentation;
import com.roadmap.securevault.dto.web.UserFilter;
import com.roadmap.securevault.dto.web.RoleUpdateRequest;
import com.roadmap.securevault.dto.web.UserUpdateRequest;
import com.roadmap.securevault.service.web.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @GetMapping
    public ResponseEntity<Page<KeycloakUserRepresentation>> getUsers(
            @ParameterObject UserFilter filter,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(userService.getUsers(pageable, filter));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<KeycloakUserRepresentation> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

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

    @PatchMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<Void> patchUserRole(
            @PathVariable UUID userId,
            @RequestBody @Valid RoleUpdateRequest request) {
        userService.addRole(userId, request.role());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<Void> deleteUserRole(
            @PathVariable UUID userId,
            @RequestBody @Valid RoleUpdateRequest request) {
        userService.removeRole(userId, request.role());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@securityEvaluator.isSelf(authentication, #id) " +
                   "or @securityEvaluator.isAbove(authentication, #id) ")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
