package trazzo.back.audit.application.port.out;

import trazzo.back.audit.domain.model.master.TenantSettingsRecord;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TenantSettingsRecordRepositoryPort {
    List<TenantSettingsRecord> findAll(String tenantSettingId, String userId, String changeReason,
        LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable);
    long count(String tenantSettingId, String userId, String changeReason,
        LocalDateTime fechaDesde, LocalDateTime fechaHasta);
    Optional<TenantSettingsRecord> findById(Long id);
}
