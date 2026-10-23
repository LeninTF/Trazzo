package trazzo.back.reports.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.reports.application.ports.in.CreateMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.in.GetMonthlyClosureDetailUseCase;
import trazzo.back.reports.application.ports.in.GetMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.in.GetMonthlyReportUseCase;
import trazzo.back.reports.application.ports.in.ListMonthlyClosureUseCase;
import trazzo.back.reports.infrastructure.adapters.in.web.dto.CreateMonthlyClosureRequest;
import trazzo.back.shared.security.TenantSecuritySlice;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static trazzo.back.shared.security.TenantSecurityContexts.actingAsPlainUser;
import static trazzo.back.shared.security.TenantSecurityContexts.actingAsTenantUserWith;

/** Los reportes y cierres mensuales son de quien exporta reportes (AUD-05). */
@WebMvcTest({MonthlyClosureController.class, MonthlyClosureDetailController.class, MonthlyReportController.class})
@AutoConfigureMockMvc(addFilters = false)
@TenantSecuritySlice
class ReportEndpointAuthorizationTest {

    private static final UUID ID = UUID.fromString("7d9e1f20-3a4b-4c5d-8e6f-7a8b9c0d1e2f");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CreateMonthlyClosureUseCase createUseCase;
    @MockitoBean
    GetMonthlyClosureUseCase getUseCase;
    @MockitoBean
    ListMonthlyClosureUseCase listUseCase;
    @MockitoBean
    GetMonthlyClosureDetailUseCase detailUseCase;
    @MockitoBean
    GetMonthlyReportUseCase reportUseCase;

    @Test
    void thePlainUserCannotSeeNorCloseMonths() throws Exception {
        actingAsPlainUser();

        mockMvc.perform(get("/reports/monthly-closures")).andExpect(status().isForbidden());
        mockMvc.perform(get("/reports/monthly-closures/" + ID)).andExpect(status().isForbidden());
        mockMvc.perform(get("/reports/monthly-closure-details/" + ID)).andExpect(status().isForbidden());
        mockMvc.perform(get("/reports/monthly-reports/" + ID)).andExpect(status().isForbidden());
        mockMvc.perform(post("/reports/monthly-closures").contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new CreateMonthlyClosureRequest(6, 2025))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(createUseCase, getUseCase, listUseCase, detailUseCase, reportUseCase);
    }

    @Test
    void anyReportExportPermissionOpensTheReports() throws Exception {
        actingAsTenantUserWith("reportes.exportar-pdf");
        mockMvc.perform(get("/reports/monthly-closures")).andExpect(status().isOk());

        actingAsTenantUserWith("reportes.exportar-excel");
        mockMvc.perform(get("/reports/monthly-closures")).andExpect(status().isOk());
    }

    @Test
    void theTenantAdministratorSeesTheReports() throws Exception {
        mockMvc.perform(get("/reports/monthly-closures")).andExpect(status().isOk());
    }
}
