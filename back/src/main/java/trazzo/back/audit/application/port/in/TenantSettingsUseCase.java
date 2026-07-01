package trazzo.back.audit.application.port.in;

import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.TenantSettingsRecordResult;

public interface TenantSettingsUseCase {
    PaginatedResult<TenantSettingsRecordResult> findAll(String tenantSettingId, String userId,
        String changeReason, String fechaDesde, String fechaHasta, int page, int size, String sort);
    TenantSettingsRecordResult findById(Long id);
}
