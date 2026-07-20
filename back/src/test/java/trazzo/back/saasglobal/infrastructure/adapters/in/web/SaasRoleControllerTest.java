package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.SaasRoleResult;
import trazzo.back.saasglobal.application.port.in.SaasRoleUseCase;
import trazzo.back.saasglobal.domain.exception.RoleInUseException;

@WebMvcTest(SaasRoleController.class)
class SaasRoleControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SaasRoleUseCase roleUseCase;

    private static SaasRoleResult result(boolean systemManaged) {
        return new SaasRoleResult(1, "soporte", "Soporte", "desc",
                List.of("monitoreo-sistema.dashboard-global"), systemManaged);
    }

    @Test
    @WithMockUser
    void listAll_returns200WithArray() throws Exception {
        when(roleUseCase.listAll()).thenReturn(List.of(result(false)));

        mockMvc.perform(get("/saas/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("soporte"));
    }

    @Test
    @WithMockUser
    void getById_returns200() throws Exception {
        when(roleUseCase.getById(1)).thenReturn(result(false));

        mockMvc.perform(get("/saas/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void create_returns201() throws Exception {
        when(roleUseCase.create(any())).thenReturn(result(false));

        mockMvc.perform(post("/saas/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"soporte","displayName":"Soporte","description":"desc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("soporte"));
    }

    @Test
    @WithMockUser
    void update_returns409WhenRoleIsSystemManaged() throws Exception {
        when(roleUseCase.update(any())).thenThrow(new RoleInUseException("El rol admin_trazzo no se puede editar"));

        mockMvc.perform(put("/saas/roles/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"admin_trazzo","displayName":"Admin","description":null}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void delete_returns204() throws Exception {
        doNothing().when(roleUseCase).deleteById(1);

        mockMvc.perform(delete("/saas/roles/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void delete_returns409WhenRoleInUse() throws Exception {
        org.mockito.Mockito.doThrow(new RoleInUseException("El rol tiene usuarios asignados"))
                .when(roleUseCase).deleteById(1);

        mockMvc.perform(delete("/saas/roles/1").with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void updatePermissions_returns200() throws Exception {
        when(roleUseCase.updatePermissions(any())).thenReturn(result(false));

        mockMvc.perform(put("/saas/roles/1/permissions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissions":["monitoreo-sistema.dashboard-global"]}
                                """))
                .andExpect(status().isOk());
    }
}
