package com.roadmap.securevault.filter;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.service.helper.AccessJwtService;
import com.roadmap.securevault.service.helper.CookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final AccessJwtService accessJwtService;
    private final UserDetailsService userDetailsService;
    private final CookieService cookieService;
    private final CookieProperties cookieProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
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
                user = userDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException ignored) {
                filterChain.doFilter(request, response);
                return;
            }
            if (!accessJwtService.isTokenValid(token, user)) {
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

//        if no valid jwt, spring boot returns 401
//        frontend will try to refresh token with /auth/refresh
//        if met with 401 again, hit /auth/logout and redirect to login page
        filterChain.doFilter(request, response);
    }
}
