package com.roadmap.securevault.filter;

import com.roadmap.securevault.service.AccessJwtService;
import com.roadmap.securevault.service.CookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {
    private AccessJwtService accessJwtService;
    private UserDetailsService userDetailsService;
    private CookieService cookieService;

    public JwtFilter(AccessJwtService accessJtService, UserDetailsService userDetails) {
        this.accessJwtService = accessJtService;
        this.userDetailsService = userDetails;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = cookieService.extractTokenFromCookie(request, "access_token");

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
