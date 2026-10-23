package trazzo.back.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import trazzo.back.shared.tenancy.TenantContext;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Autentica al agente biométrico con el token de su dispositivo ("Bearer trzd_…"): fija el TenantContext del
 * tenant dueño del token y un DeviceAgentPrincipal con ROLE_AGENT. Va antes de JwtAuthFilter; cualquier otro
 * Authorization lo deja pasar sin tocarlo. El agente solo llega a sus endpoints: el resto responde 403.
 */
@Slf4j
public class DeviceTokenAuthFilter extends OncePerRequestFilter {

    public static final String TOKEN_PREFIX = "trzd_";
    private static final String BEARER = "Bearer ";

    private static final Set<String> AGENT_ENDPOINTS = Set.of(
            "POST /asistencia/marcar",
            "POST /asistencia/sync",
            "GET /corehr/biometria/enroll/pendiente",
            "POST /corehr/biometria/enroll/completar",
            "GET /security/public-key");

    private final AgentDeviceTokenRepository tokens;

    public DeviceTokenAuthFilter(AgentDeviceTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER + TOKEN_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<DeviceAgentPrincipal> agent;
        try {
            agent = tokens.findActive(header.substring(BEARER.length()));
        } catch (DataAccessException e) {
            log.warn("No se pudo validar un token de dispositivo [{}] — path={}",
                    e.getClass().getSimpleName(), pathOf(request));
            reject(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        if (agent.isEmpty()) {
            log.warn("Token de dispositivo desconocido o revocado — path={}", pathOf(request));
            reject(response, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (!AGENT_ENDPOINTS.contains(request.getMethod() + " " + pathOf(request))) {
            log.warn("Token de dispositivo fuera de los endpoints del agente — {} {}", request.getMethod(), pathOf(request));
            reject(response, HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            TenantContext.set(agent.get().schemaName());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    agent.get(), null, List.of(new SimpleGrantedAuthority(DeviceAgentPrincipal.AUTHORITY))));
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    // Igual que los RequestMatcher de Spring Security: servletPath + pathInfo (con el DispatcherServlet en "/"
    // la ruta llega en el primero; mapeado como "/*" o en MockMvc, en el segundo).
    private static String pathOf(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null ? request.getServletPath() : request.getServletPath() + pathInfo;
    }

    private static void reject(HttpServletResponse response, int status) {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        response.setStatus(status);
    }
}
