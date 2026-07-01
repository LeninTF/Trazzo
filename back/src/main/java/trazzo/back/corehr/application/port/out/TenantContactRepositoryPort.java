package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.employee.TenantContact;

import java.util.List;
import java.util.Optional;

public interface TenantContactRepositoryPort {
    TenantContact save(TenantContact tenantContact);
    Optional<TenantContact> findById(Long id);
    List<TenantContact> findAll(int page, int size);
    long count();
    List<TenantContact> findByTenantUserId(Long tenantUserId);
}
