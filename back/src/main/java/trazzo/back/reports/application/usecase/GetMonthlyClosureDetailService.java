package trazzo.back.reports.application.usecase;

import trazzo.back.reports.application.dto.result.MonthlyClosureDetailResult;
import trazzo.back.reports.application.ports.in.GetMonthlyClosureDetailUseCase;
import trazzo.back.reports.application.ports.out.MonthlyClosureDetailRepositoryPort;
import trazzo.back.reports.domain.exception.MonthlyClosureDetailNotFoundException;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.util.UUID;

public class GetMonthlyClosureDetailService implements GetMonthlyClosureDetailUseCase {

    private final MonthlyClosureDetailRepositoryPort detailRepository;

    public GetMonthlyClosureDetailService(MonthlyClosureDetailRepositoryPort detailRepository) {
        this.detailRepository = detailRepository;
    }

    @Override
    public MonthlyClosureDetailResult execute(UUID id) {
        MonthlyClosureDetail detail = detailRepository.findById(id)
                .orElseThrow(() -> new MonthlyClosureDetailNotFoundException(id));
        return new MonthlyClosureDetailResult(
                detail.getId(), detail.getMonthClosureId(), detail.getTenantUserFullName(),
                detail.getTenantUserDocument(), detail.getDepartmentName(),
                detail.getRoleName(), detail.getTotalWorkedHours(),
                detail.getTotalTardinessMinutes(), detail.getTotalAbsences(),
                detail.getTotalOvertimeHours());
    }
}
