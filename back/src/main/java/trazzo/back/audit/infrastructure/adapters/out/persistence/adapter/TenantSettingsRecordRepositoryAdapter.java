package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.audit.application.port.out.TenantSettingsRecordRepositoryPort;
import trazzo.back.audit.domain.model.master.TenantSettingsRecord;
import trazzo.back.audit.infrastructure.adapters.out.persistence.mapper.TenantSettingsRecordMapper;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.TenantSettingsRecordJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantSettingsRecordRepositoryAdapter implements TenantSettingsRecordRepositoryPort {

    private final TenantSettingsRecordJpaRepository jpaRepository;

    @Override
    public List<TenantSettingsRecord> findAll(String tenantSettingId, String userId, String changeReason,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable) {
        return jpaRepository.findByFilters(tenantSettingId, userId, changeReason, fechaDesde, fechaHasta, pageable)
                .stream()
                .map(TenantSettingsRecordMapper::toDomain)
                .toList();
    }

    @Override
    public long count(String tenantSettingId, String userId, String changeReason,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        return jpaRepository.findByFilters(tenantSettingId, userId, changeReason, fechaDesde, fechaHasta, Pageable.unpaged())
                .getTotalElements();
    }

    @Override
    public Optional<TenantSettingsRecord> findById(Long id) {
        return jpaRepository.findById(id)
                .map(TenantSettingsRecordMapper::toDomain);
    }
}
