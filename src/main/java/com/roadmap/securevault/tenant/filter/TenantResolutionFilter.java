package com.roadmap.securevault.tenant.filter;

import com.roadmap.securevault.multitenancy.TenantContext;
import com.roadmap.securevault.tenant.entity.Tenant;
import com.roadmap.securevault.tenant.entity.enums.TenantStatus;
import com.roadmap.securevault.tenant.repo.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String companyCode = extractCompanyCode(path);

        if (companyCode == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Tenant> tenantOpt = tenantRepository.findByCompanyCode(companyCode);
        if (tenantOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Tenant not found\"}");
            return;
        }

        Tenant tenant = tenantOpt.get();

        if (tenant.getStatus() != TenantStatus.APPROVED) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Tenant is not active\"}");
            return;
        }

        TenantContext.setTenantSchema(tenant.getSchemaName());
        TenantContext.setCompanyCode(tenant.getCompanyCode());
        request.setAttribute("tenant", tenant);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractCompanyCode(String path) {
        if (path == null || !path.startsWith("/t/")) return null;
        String[] parts = path.split("/");
        return parts.length >= 3 ? parts[2] : null;
    }
}