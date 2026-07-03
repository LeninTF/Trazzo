package trazzo.back.reports.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.reports.application.dto.result.MonthlyClosureDetailResult;
import trazzo.back.reports.application.dto.result.MonthlyClosureWithDetailsResult;
import trazzo.back.reports.application.ports.in.GetMonthlyReportUseCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@WebMvcTest(controllers = MonthlyReportController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class MonthlyReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMonthlyReportUseCase reportUseCase;

    @Test
    void shouldReturnReportWithDetails() throws Exception {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetailResult detail = new MonthlyClosureDetailResult(
                UUID.randomUUID(), id, 1L, "Juan", "123", "TI", "Dev", 160.0, 10, 1, 5.0, now);
        MonthlyClosureWithDetailsResult result = new MonthlyClosureWithDetailsResult(
                id, 6, 2025, 1, "excel", "pdf", now, List.of(detail));

        when(reportUseCase.execute(any(UUID.class))).thenReturn(result);

        mockMvc.perform(get("/reports/monthly-reports/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.month").value(6))
                .andExpect(jsonPath("$.details.length()").value(1));
    }

    @Test
    void shouldReturnReportWithEmptyDetails() throws Exception {
        UUID id = UUID.randomUUID();
        MonthlyClosureWithDetailsResult result = new MonthlyClosureWithDetailsResult(
                id, 6, 2025, 0, null, null, LocalDateTime.now(), List.of());

        when(reportUseCase.execute(any(UUID.class))).thenReturn(result);

        mockMvc.perform(get("/reports/monthly-reports/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.length()").value(0));
    }
}
