package trazzo.back.dashboard.application.port.in;

import trazzo.back.dashboard.domain.model.DashboardSummary;

import java.time.LocalDate;

public interface DashboardSummaryUseCase {
    DashboardSummary getSummary(LocalDate desde, LocalDate hasta);
}
