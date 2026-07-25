package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.SoftDeleteResult;
import trazzo.back.corehr.application.dto.result.TenantUserProfileResult;
import trazzo.back.corehr.application.port.in.TenantUserUseCase;
import trazzo.back.shared.security.AuthenticatedUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantUserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TenantUserUseCase tenantUserUseCase;

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 10, 30);

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private static TenantUserProfileResult aResult() {
        var persona = new TenantUserProfileResult.PersonaResult(1, "DNI", "12345678", "Juan", "Perez", "Lopez", "1990-01-15", null);
        var rol = new TenantUserProfileResult.RoleInfoResult("role-1", "ADMIN");
        var sede = new TenantUserProfileResult.OrgAssignment(1L, "Sede Central");
        var area = new TenantUserProfileResult.OrgAssignment(10L, "Tecnologia");
        var depto = new TenantUserProfileResult.OrgAssignment(100L, "Desarrollo");
        return new TenantUserProfileResult(
                1L, "juan@trazzo.pe", "999888777", "ACTIVO", false,
                NOW, NOW, persona, rol,
                List.of(sede), List.of(area), List.of(depto)
        );
    }

    private static AuthenticatedUser testPrincipal() {
        return new AuthenticatedUser(UUID.randomUUID(), "juan@trazzo.pe", "pass", List.of(), true);
    }

    private static void mockPrincipal(AuthenticatedUser user) {
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static String fullCreateJson() {
        return """
                {
                  "documentType": "DNI",
                  "documentValue": "12345678",
                  "name": "Juan",
                  "fatherSurname": "Perez",
                  "motherSurname": "Lopez",
                  "birthDate": "1990-01-15",
                  "imgUrl": "https://img.test.com/juan.png",
                  "email": "juan@trazzo.pe",
                  "phone": "999888777",
                  "roleId": "role-1",
                  "sedeIds": [1],
                  "areaIds": [10],
                  "departamentoIds": [100]
                }
                """;
    }

    // ──────────────────────────────────────────────────────────
    // GET /usuarios — list
    // ──────────────────────────────────────────────────────────

    @Test
    void list_withDefaultParams_shouldReturn200() throws Exception {
        var paginated = new PaginatedResult<TenantUserProfileResult>(List.of(aResult()), 0, 20, 1, 1);
        when(tenantUserUseCase.findAll(isNull(), isNull(), eq(0), eq(20), isNull()))
                .thenReturn(paginated);

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("juan@trazzo.pe"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_withSearchParams_shouldReturn200() throws Exception {
        var paginated = new PaginatedResult<TenantUserProfileResult>(List.of(aResult()), 0, 10, 1, 1);
        when(tenantUserUseCase.findAll(eq("juan"), eq("ACTIVO"), eq(0), eq(10), eq("name,asc")))
                .thenReturn(paginated);

        mockMvc.perform(get("/usuarios")
                        .param("search", "juan")
                        .param("status", "ACTIVO")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").value("juan@trazzo.pe"));
    }

    // ──────────────────────────────────────────────────────────
    // GET /usuarios/{id}
    // ──────────────────────────────────────────────────────────

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(tenantUserUseCase.findById(1L)).thenReturn(Optional.of(aResult()));

        mockMvc.perform(get("/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("juan@trazzo.pe"))
                .andExpect(jsonPath("$.persona.name").value("Juan"))
                .andExpect(jsonPath("$.rol.name").value("ADMIN"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(tenantUserUseCase.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/usuarios/999"))
                .andExpect(status().isNotFound());
    }

    // ──────────────────────────────────────────────────────────
    // POST /usuarios — create
    // ──────────────────────────────────────────────────────────

    @Test
    void create_shouldReturn201() throws Exception {
        when(tenantUserUseCase.create(any())).thenReturn(aResult());

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fullCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("juan@trazzo.pe"));
    }

    @Test
    void create_shouldReturn400WhenDocumentTypeIsBlank() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentValue": "12345678",
                                  "name": "Juan",
                                  "fatherSurname": "Perez",
                                  "motherSurname": "Lopez",
                                  "email": "juan@trazzo.pe",
                                  "roleId": "role-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "DNI",
                                  "documentValue": "12345678",
                                  "fatherSurname": "Perez",
                                  "motherSurname": "Lopez",
                                  "email": "juan@trazzo.pe",
                                  "roleId": "role-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenRoleIdIsNull() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "DNI",
                                  "documentValue": "12345678",
                                  "name": "Juan",
                                  "fatherSurname": "Perez",
                                  "motherSurname": "Lopez",
                                  "email": "juan@trazzo.pe"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ──────────────────────────────────────────────────────────
    // PUT /usuarios/{id} — update
    // ──────────────────────────────────────────────────────────

    @Test
    void update_shouldReturn200() throws Exception {
        when(tenantUserUseCase.update(eq(1L), any())).thenReturn(aResult());

        mockMvc.perform(put("/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fullCreateJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("juan@trazzo.pe"));
    }

    @Test
    void update_shouldReturn400WhenNotFound() throws Exception {
        when(tenantUserUseCase.update(eq(999L), any()))
                .thenThrow(new IllegalArgumentException("TenantUser not found: 999"));

        mockMvc.perform(put("/usuarios/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fullCreateJson()))
                .andExpect(status().isBadRequest());
    }

    // ──────────────────────────────────────────────────────────
    // PATCH /usuarios/{id} — patch
    // ──────────────────────────────────────────────────────────

    @Test
    void patch_shouldReturn200() throws Exception {
        when(tenantUserUseCase.patch(eq(1L), any())).thenReturn(aResult());

        mockMvc.perform(patch("/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "999000111",
                                  "email": "nuevo@trazzo.pe"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ──────────────────────────────────────────────────────────
    // DELETE /usuarios/{id}
    // ──────────────────────────────────────────────────────────

    @Test
    void delete_shouldReturn200() throws Exception {
        var softDelete = new SoftDeleteResult(1L, "INACTIVO", NOW);
        when(tenantUserUseCase.delete(1L)).thenReturn(softDelete);

        mockMvc.perform(delete("/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("INACTIVO"));
    }

    // ──────────────────────────────────────────────────────────
    // PUT /usuarios/{id}/rol — assign role
    // ──────────────────────────────────────────────────────────

    @Test
    void assignRole_shouldReturn200() throws Exception {
        when(tenantUserUseCase.assignRole(1L, "role-2")).thenReturn(aResult());

        mockMvc.perform(put("/usuarios/1/rol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleId": "role-2"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void assignRole_shouldReturn400WhenRoleIdIsNull() throws Exception {
        mockMvc.perform(put("/usuarios/1/rol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ──────────────────────────────────────────────────────────
    // PATCH /usuarios/{id}/password — change password
    // ──────────────────────────────────────────────────────────

    @Test
    void changePassword_shouldReturn204() throws Exception {
        doNothing().when(tenantUserUseCase).changePassword(1L, "oldPass", "newPass");

        mockMvc.perform(patch("/usuarios/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "oldPass",
                                  "newPassword": "newPass"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    // ──────────────────────────────────────────────────────────
    // GET /usuarios/me
    // ──────────────────────────────────────────────────────────

    @Test
    void getMe_shouldReturn200() throws Exception {
        var principal = testPrincipal();
        mockPrincipal(principal);
        when(tenantUserUseCase.findMe(principal.id().toString())).thenReturn(Optional.of(aResult()));

        mockMvc.perform(get("/usuarios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@trazzo.pe"));
    }

    @Test
    void getMe_shouldReturn404WhenNotFound() throws Exception {
        var principal = testPrincipal();
        mockPrincipal(principal);
        when(tenantUserUseCase.findMe(principal.id().toString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/usuarios/me"))
                .andExpect(status().isNotFound());
    }

    // ──────────────────────────────────────────────────────────
    // PATCH /usuarios/me
    // ──────────────────────────────────────────────────────────

    @Test
    void patchMe_shouldReturn200() throws Exception {
        var principal = testPrincipal();
        mockPrincipal(principal);
        when(tenantUserUseCase.patchMe(principal.id().toString(), "111222333", "https://img.test.com/new.png"))
                .thenReturn(aResult());

        mockMvc.perform(patch("/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "111222333",
                                  "imgUrl": "https://img.test.com/new.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@trazzo.pe"));
    }
}
