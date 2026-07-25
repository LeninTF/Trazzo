package trazzo.back.audit.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.TenantSettingsRecordResult;
import trazzo.back.audit.application.port.in.TenantSettingsUseCase;
import trazzo.back.audit.application.port.out.TenantSettingsRecordRepositoryPort;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.master.TenantSettingsRecord;
import trazzo.back.shared.util.SortUtils;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RequiredArgsConstructor
public class TenantSettingsService implements TenantSettingsUseCase {

    private final TenantSettingsRecordRepositoryPort settingsRepository;

    @Override
    public PaginatedResult<TenantSettingsRecordResult> findAll(String tenantSettingId, String userId,
            String changeReason, String fechaDesde, String fechaHasta, int page, int size, String sort) {
        LocalDateTime desde = fechaDesde != null ? LocalDate.parse(fechaDesde).atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? LocalDate.parse(fechaHasta).atTime(LocalTime.MAX) : null;
        var pageable = PageRequest.of(page, size, SortUtils.parseSort(sort, f -> f));
        var records = settingsRepository.findAll(tenantSettingId, userId, changeReason, desde, hasta, pageable);
        var total = settingsRepository.count(tenantSettingId, userId, changeReason, desde, hasta);
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        var results = records.stream().map(this::toResult).toList();
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public TenantSettingsRecordResult findById(Long id) {
        return settingsRepository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new AuditNotFoundException("Tenant settings record not found: " + id));
    }

    private TenantSettingsRecordResult toResult(TenantSettingsRecord record) {
        return new TenantSettingsRecordResult(
                record.getId(), record.getTenantSettingId(), record.getDbName(),
                record.getDbHost(), record.getDbUser(), record.getUserId(),
                record.getChangeReason(), record.getCreatedAt()
        );
    }
}
