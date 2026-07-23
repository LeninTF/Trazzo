package trazzo.back.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import trazzo.back.shared.tenancy.TenantContext;

/**
 * Blocks requests to tenant-specific endpoints when the caller has no tenant context
 * (i.e. TenantContext is "public"). This prevents SaaS admins and unauthenticated
 * tenant-scoped users from hitting endpoints that would query non-existent tenant
 * tables, resulting in 500 errors or confusing 403s from @PreAuthorize.
 *
 * Only activates against PostgreSQL (production). In H2 test environments the check
 * is skipped, since tenant schema routing is not relevant there.
 *
 * Must run AFTER JwtAuthFilter (which sets TenantContext) and BEFORE controllers.
 */
@Slf4j
public class TenantContextGuard extends OncePerRequestFilter {

    private static final Set<String> NON_TENANT_PREFIXES = Set.of(
            "/auth/",
            "/saas/",
            "/public/",
            "/actuator/",
            "/security/",
            "/audit/",
            "/tenants/",
            "/requests",
            "/error",
            "/webhooks/",
            "/shop/"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataSource dataSource;
    private Boolean isPostgreSQL;

    public TenantContextGuard(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isNonTenantPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!isPostgreSQL()) {
            filterChain.doFilter(request, response);
            return;
        }

        String schema = TenantContext.get();
        if ("public".equals(schema)) {
            log.warn("Blocking tenant endpoint for user without tenant context — path={}, schema={}",
                    path, schema);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            new ErrorResponse("Acceso denegado: se requiere contexto de tenant")));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isNonTenantPath(String path) {
        return NON_TENANT_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isPostgreSQL() {
        if (isPostgreSQL != null) {
            return isPostgreSQL;
        }
        try {
            isPostgreSQL = "PostgreSQL".equals(
                    dataSource.getConnection().getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            log.warn("Could not determine database product name, assuming non-PostgreSQL", e);
            isPostgreSQL = false;
        }
        return isPostgreSQL;
    }

    private record ErrorResponse(String message) {}
}
