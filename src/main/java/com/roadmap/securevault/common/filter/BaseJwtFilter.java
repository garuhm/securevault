package com.roadmap.securevault.common.filter;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.service.AccessJwtService;
import com.roadmap.securevault.common.service.BaseUserService;
import com.roadmap.securevault.common.service.CookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public abstract class BaseJwtFilter extends OncePerRequestFilter {

    protected final AccessJwtService accessJwtService;
    protected final BaseUserService<?, ?> userService;
    protected final CookieService cookieService;
    protected final CookieProperties cookieProperties;

    protected BaseJwtFilter(AccessJwtService accessJwtService,
                            BaseUserService<?, ?> userService,
                            CookieService cookieService,
                            CookieProperties cookieProperties) {
        this.accessJwtService = accessJwtService;
        this.userService = userService;
        this.cookieService = cookieService;
        this.cookieProperties = cookieProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = cookieService.extractTokenFromCookie(request, cookieProperties.accessTokenCookieName());

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = accessJwtService.extractUsername(token);
            if (username == null || username.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            UserDetails user;
            try {
                user = userService.loadUserByUsername(username);
            } catch (UsernameNotFoundException ignored) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!accessJwtService.isTokenValid(token, user)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!additionalValidation(token, user, request)) {
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    user.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (RuntimeException ignored) {
            // Invalid token: proceed unauthenticated and let SecurityConfig decide.
        }

        filterChain.doFilter(request, response);
    }

    protected boolean additionalValidation(String token, UserDetails user, HttpServletRequest request) {
        return true;
    }
}