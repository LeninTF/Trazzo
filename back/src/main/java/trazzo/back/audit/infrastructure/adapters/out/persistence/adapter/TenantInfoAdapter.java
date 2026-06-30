package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.springframework.stereotype.Component;
import trazzo.back.audit.application.port.out.TenantInfoPort;

import java.util.Optional;

@Component
public class TenantInfoAdapter implements TenantInfoPort {

    @Override
    public Optional<TenantInfo> findByTenantId(String tenantId) {
        return Optional.empty();
    }
}
