package com.roadmap.securevault.common.controller;

import com.roadmap.securevault.common.entity.BaseUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;

public abstract class BaseMeController<U extends BaseUser & UserDetails, R> {

    @GetMapping("/me")
    public ResponseEntity<R> getMe() {
        U user = (U) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(toMeResponse(user));
    }

    protected abstract R toMeResponse(U user);
}