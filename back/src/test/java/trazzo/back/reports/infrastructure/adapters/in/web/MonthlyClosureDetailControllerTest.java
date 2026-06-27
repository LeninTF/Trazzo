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
import trazzo.back.reports.application.ports.in.GetMonthlyClosureDetailUseCase;

import java.time.LocalDateTime;
import java.util.UUID;

@WebMvcTest(controllers = MonthlyClosureDetailController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class MonthlyClosureDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMonthlyClosureDetailUseCase detailUseCase;

    @Test
    void shouldReturnDetailById() throws Exception {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        MonthlyClosureDetailResult result = new MonthlyClosureDetailResult(
                id, closureId, "Juan Perez", "12345678", "TI", "Developer",
                160.0, 10.0, 1, 5.0, LocalDateTime.now());

        when(detailUseCase.execute(any(UUID.class))).thenReturn(result);

        mockMvc.perform(get("/api/v1/reports/monthly-closure-details/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.tenantUserFullName").value("Juan Perez"))
                .andExpect(jsonPath("$.totalWorkedHours").value(160.0));
    }
}
