package trazzo.back.audit.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.TenantSettingsRecordResult;
import trazzo.back.audit.application.port.in.TenantSettingsUseCase;
import trazzo.back.audit.domain.exception.AuditNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantSettingsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TenantSettingsUseCase tenantSettingsUseCase;

    private static TenantSettingsRecordResult aResult() {
        return new TenantSettingsRecordResult(1L, "ts-1", "dbName", "dbHost",
                "dbUser", "u-1", "reason", LocalDateTime.now());
    }

    @Test
    void list_shouldReturn200WithTenantSettingsRecordListResponse() throws Exception {
        var paginated = new PaginatedResult<TenantSettingsRecordResult>(List.of(aResult()), 0, 10, 1, 1);
        when(tenantSettingsUseCase.findAll(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(paginated);

        mockMvc.perform(get("/audit/tenant-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].tenant_setting_id").value("ts-1"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(tenantSettingsUseCase.findById(1L)).thenReturn(aResult());

        mockMvc.perform(get("/audit/tenant-settings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tenant_setting_id").value("ts-1"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(tenantSettingsUseCase.findById(anyLong())).thenThrow(new AuditNotFoundException("Tenant settings record not found: 999"));

        mockMvc.perform(get("/audit/tenant-settings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
