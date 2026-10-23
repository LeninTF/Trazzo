package trazzo.back.organization.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.port.in.AreaUseCase;
import trazzo.back.organization.application.port.in.BranchUseCase;
import trazzo.back.organization.application.port.in.DepartmentUseCase;
import trazzo.back.organization.application.port.in.PermissionUseCase;
import trazzo.back.organization.application.port.in.RolePermissionsUseCase;
import trazzo.back.organization.application.port.in.RoleUseCase;
import trazzo.back.organization.application.port.in.UserRoleUseCase;
import trazzo.back.shared.security.TenantSecuritySlice;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static trazzo.back.shared.security.TenantSecurityContexts.actingAsPlainUser;
import static trazzo.back.shared.security.TenantSecurityContexts.actingAsTenantUserWith;

/** Matriz permiso → endpoint de organización (AUD-05; S10 y S11 de la auditoría). */
@WebMvcTest({AreaController.class, BranchController.class, DepartmentController.class, PermissionController.class,
        RoleController.class, UserRoleController.class})
@AutoConfigureMockMvc(addFilters = false)
@TenantSecuritySlice
class OrganizationEndpointAuthorizationTest {

    private static final String NEW_PERMISSION_JSON =
            "{\"name\":\"READ\",\"description\":\"desc\",\"masterFeaturesCode\":\"CODE\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AreaUseCase areaUseCase;
    @MockitoBean
    BranchUseCase branchUseCase;
    @MockitoBean
    DepartmentUseCase departmentUseCase;
    @MockitoBean
    PermissionUseCase permissionUseCase;
    @MockitoBean
    RoleUseCase roleUseCase;
    @MockitoBean
    RolePermissionsUseCase rolePermissionsUseCase;
    @MockitoBean
    UserRoleUseCase userRoleUseCase;

    private static <T> PaginatedResult<T> emptyPage() {
        return new PaginatedResult<>(List.of(), 0, 20, 0L, 0);
    }

    @Test
    void thePlainUserCannotSeeRolesNorCreatePermissions() throws Exception {
        actingAsPlainUser();

        mockMvc.perform(get("/org/roles")).andExpect(status().isForbidden());
        mockMvc.perform(get("/org/permissions")).andExpect(status().isForbidden());
        mockMvc.perform(post("/org/permissions").contentType(MediaType.APPLICATION_JSON).content(NEW_PERMISSION_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/org/roles/r-1/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/org/users/10/roles")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/org/users/10/roles/1")).andExpect(status().isForbidden());
        verifyNoInteractions(roleUseCase, permissionUseCase, rolePermissionsUseCase, userRoleUseCase);
    }

    @Test
    void thePlainUserCannotSeeNorChangeTheStructure() throws Exception {
        actingAsPlainUser();

        mockMvc.perform(get("/org/branches")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/org/branches/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/org/areas")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/org/areas/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/org/departments")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/org/departments/1")).andExpect(status().isForbidden());
        verifyNoInteractions(branchUseCase, areaUseCase, departmentUseCase);
    }

    @Test
    void aRoleAssignerAssignsRolesButOnlyTheAdministratorChangesThem() throws Exception {
        actingAsTenantUserWith("gestion-trabajadores.asignar-roles");
        when(roleUseCase.findAll(any(), anyInt(), anyInt(), any())).thenReturn(emptyPage());

        mockMvc.perform(get("/org/roles")).andExpect(status().isOk());
        mockMvc.perform(delete("/org/users/10/roles/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/org/roles/r-1")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/org/roles/r-1/permissions/p-1")).andExpect(status().isForbidden());
        mockMvc.perform(post("/org/permissions").contentType(MediaType.APPLICATION_JSON).content(NEW_PERMISSION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void branchesAndAreasHaveSeparatePermissions() throws Exception {
        actingAsTenantUserWith("estructura-organizacional.crear-editar-sedes");
        mockMvc.perform(delete("/org/branches/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/org/areas/1")).andExpect(status().isForbidden());

        actingAsTenantUserWith("estructura-organizacional.crear-editar-areas");
        mockMvc.perform(delete("/org/areas/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/org/departments/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/org/branches/1")).andExpect(status().isForbidden());
    }

    @Test
    void scheduleManagersSeeTheStructureButCannotChangeIt() throws Exception {
        actingAsTenantUserWith("gestion-horarios.configurar-turnos");
        when(branchUseCase.findAll(any(), any(), anyInt(), anyInt(), any())).thenReturn(emptyPage());

        mockMvc.perform(get("/org/branches")).andExpect(status().isOk());
        mockMvc.perform(delete("/org/branches/1")).andExpect(status().isForbidden());
    }

    @Test
    void theTenantAdministratorManagesRolesPermissionsAndStructure() throws Exception {
        mockMvc.perform(delete("/org/roles/r-1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/org/roles/r-1/permissions/p-1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/org/permissions/p-1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/org/branches/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/org/departments/1")).andExpect(status().isNoContent());
    }
}
