package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.port.out.TenantContactRepositoryPort;
import trazzo.back.corehr.domain.model.employee.TenantContact;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.TenantContactMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.TenantContactJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantContactRepositoryAdapter implements TenantContactRepositoryPort {

    private final TenantContactJpaRepository tenantContactRepo;

    @Override
    @Transactional
    public TenantContact save(TenantContact tenantContact) {
        var entity = TenantContactMapper.toEntity(tenantContact);
        var saved = tenantContactRepo.save(entity);
        return TenantContactMapper.toDomain(saved);
    }

    @Override
    public Optional<TenantContact> findById(Long id) {
        return tenantContactRepo.findById(id).map(TenantContactMapper::toDomain);
    }

    @Override
    public List<TenantContact> findAll(int page, int size) {
        var pageable = PageRequest.of(page, size);
        return tenantContactRepo.findAll(pageable)
                .stream()
                .map(TenantContactMapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return tenantContactRepo.count();
    }

    @Override
    public List<TenantContact> findByTenantUserId(Long tenantUserId) {
        return tenantContactRepo.findByTenantUserId(tenantUserId)
                .stream()
                .map(TenantContactMapper::toDomain)
                .toList();
    }
}
