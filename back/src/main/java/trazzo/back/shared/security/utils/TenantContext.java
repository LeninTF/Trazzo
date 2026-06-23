package trazzo.back.shared.security.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import trazzo.back.shared.security.TenantAuthenticationDetails;

@Component
public class TenantContext {

    public Long getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof TenantAuthenticationDetails details) {
            return details.tenantId();
        }
        throw new IllegalStateException("No hay tenant autenticado en el contexto de seguridad");
    }

    public boolean belongsToCurrentTenant(Long tenantId) {
        return getCurrentTenantId().equals(tenantId);
    }
}
