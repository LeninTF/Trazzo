package trazzo.back.audit.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.audit.application.dto.result.LogInHistoryResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.port.in.LoginHistoryUseCase;
import trazzo.back.audit.application.port.out.LogInHistoryRepositoryPort;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.master.LogInHistory;
import trazzo.back.audit.domain.model.master.StatusLogin;
import trazzo.back.audit.infrastructure.adapters.out.persistence.adapter.SortUtils;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RequiredArgsConstructor
public class LoginHistoryService implements LoginHistoryUseCase {

    private final LogInHistoryRepositoryPort loginHistoryRepository;

    @Override
    public PaginatedResult<LogInHistoryResult> findAll(String userId, String attemptedEmail,
            StatusLogin status, String fechaDesde, String fechaHasta, int page, int size, String sort) {
        LocalDateTime desde = fechaDesde != null ? LocalDate.parse(fechaDesde).atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? LocalDate.parse(fechaHasta).atTime(LocalTime.MAX) : null;
        var pageable = PageRequest.of(page, size, SortUtils.parseSort(sort, f -> f));
        var logs = loginHistoryRepository.findAll(userId, attemptedEmail, status, desde, hasta, pageable);
        var total = loginHistoryRepository.count(userId, attemptedEmail, status, desde, hasta);
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        var results = logs.stream().map(this::toResult).toList();
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public LogInHistoryResult findById(Long id) {
        return loginHistoryRepository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new AuditNotFoundException("Login history not found: " + id));
    }

    private LogInHistoryResult toResult(LogInHistory log) {
        return new LogInHistoryResult(
                log.getId(), log.getUserId(), log.getAttemptedEmail(),
                log.getStatus(), log.getIpAddress(), log.getUserAgent(),
                log.getCreatedAt()
        );
    }
}
