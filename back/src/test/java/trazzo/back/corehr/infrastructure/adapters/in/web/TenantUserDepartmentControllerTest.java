package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.command.CreateTenantUserDepartmentCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantUserDepartmentCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.TenantUserDepartmentResult;
import trazzo.back.corehr.application.port.in.TenantUserDepartmentUseCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantUserDepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantUserDepartmentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TenantUserDepartmentUseCase tenantUserDepartmentUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDate START = LocalDate.of(2025, 1, 1);

    private static TenantUserDepartmentResult aResult() {
        return new TenantUserDepartmentResult(1L, 10L, 5L, "IT", true, START, null, NOW, NOW);
    }

    @Test
    void listByUser_shouldReturn200() throws Exception {
        var paginated = new PaginatedResult<TenantUserDepartmentResult>(List.of(aResult()), 0, 20, 1, 1);
        when(tenantUserDepartmentUseCase.findAllByTenantUserId(eq(10L), anyInt(), anyInt())).thenReturn(paginated);

        mockMvc.perform(get("/corehr/usuarios/10/departamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].department_name").value("IT"))
                .andExpect(jsonPath("$.content[0].is_primary").value(true));
    }

    @Test
    void listByUser_shouldReturnEmptyList() throws Exception {
        var empty = new PaginatedResult<TenantUserDepartmentResult>(List.of(), 0, 20, 0, 0);
        when(tenantUserDepartmentUseCase.findAllByTenantUserId(eq(99L), anyInt(), anyInt())).thenReturn(empty);

        mockMvc.perform(get("/corehr/usuarios/99/departamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(tenantUserDepartmentUseCase.create(anyLong(), any(CreateTenantUserDepartmentCommand.class)))
                .thenReturn(aResult());

        mockMvc.perform(post("/corehr/usuarios/10/departamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"department_id": 5, "start_date": "2025-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.department_name").value("IT"));
    }

    @Test
    void create_shouldReturn400WhenDepartmentIdIsNull() throws Exception {
        mockMvc.perform(post("/corehr/usuarios/10/departamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"start_date": "2025-01-01"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenStartDateIsNull() throws Exception {
        mockMvc.perform(post("/corehr/usuarios/10/departamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"department_id": 5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_shouldReturn200() throws Exception {
        when(tenantUserDepartmentUseCase.patch(anyLong(), anyLong(), any(PatchTenantUserDepartmentCommand.class)))
                .thenReturn(aResult());

        mockMvc.perform(patch("/corehr/usuarios/10/departamentos/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"is_primary": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department_name").value("IT"));
    }
}
