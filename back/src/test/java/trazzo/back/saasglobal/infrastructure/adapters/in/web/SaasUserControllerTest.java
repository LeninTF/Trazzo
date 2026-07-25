package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.SaasUserResult;
import trazzo.back.saasglobal.application.port.in.SaasUserUseCase;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.SecurityConfig;

@WebMvcTest(SaasUserController.class)
@Import(SecurityConfig.class)
class SaasUserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SaasUserUseCase userUseCase;

    private static final UUID PRINCIPAL_ID = UUID.randomUUID();

    private static UsernamePasswordAuthenticationToken adminAuth() {
        var authUser = new AuthenticatedUser(PRINCIPAL_ID, "admin@trazzo.pe", "pass", List.of(), true);
        return new UsernamePasswordAuthenticationToken(authUser, null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SAAS_ADMIN")));
    }

    private static SaasUserResult result(String id) {
        var person = new SaasUserResult.PersonSummary(1, null, "DNI", "00000000", "Ana", "Perez", "Lopez");
        var roles = List.of(new SaasUserResult.RoleTag(2, "soporte", "desc"));
        return new SaasUserResult(id, "ana@example.com", null, false, LocalDateTime.now(), person, roles);
    }

    @Test
    void listAll_returns200() throws Exception {
        when(userUseCase.listAll(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(PaginatedResult.of(List.of(result("user-1")), 0, 20, 1));

        mockMvc.perform(get("/saas/users").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("user-1"))
                .andExpect(jsonPath("$.content[0].email").value("ana@example.com"))
                .andExpect(jsonPath("$.content[0].persona.father_surname").value("Perez"))
                .andExpect(jsonPath("$.content[0].roles[0].name").value("soporte"));
    }

    @Test
    void me_returnsCurrentPrincipalProfile() throws Exception {
        when(userUseCase.getById(PRINCIPAL_ID.toString())).thenReturn(result(PRINCIPAL_ID.toString()));

        mockMvc.perform(get("/saas/users/me").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void create_returns201() throws Exception {
        when(userUseCase.create(any())).thenReturn(result("user-1"));

        mockMvc.perform(post("/saas/users")
                        .with(authentication(adminAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"document_type":"DNI","document_value":"00000000","name":"Ana",
                                 "father_surname":"Perez","mother_surname":"Lopez","email":"ana@example.com",
                                 "role_ids":[2]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user-1"));
    }

    @Test
    void create_returns400WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/saas/users")
                        .with(authentication(adminAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(userUseCase).deleteById("user-1");

        mockMvc.perform(delete("/saas/users/user-1").with(authentication(adminAuth())).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void assignRoles_returns200() throws Exception {
        when(userUseCase.assignRoles(any())).thenReturn(result("user-1"));

        mockMvc.perform(put("/saas/users/user-1/roles")
                        .with(authentication(adminAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role_ids":[2,3]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void listAll_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/saas/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_returns200() throws Exception {
        when(userUseCase.getById("user-1")).thenReturn(result("user-1"));

        mockMvc.perform(get("/saas/users/user-1").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"));
    }

    @Test
    void update_returns200() throws Exception {
        when(userUseCase.update(any())).thenReturn(result("user-1"));

        mockMvc.perform(patch("/saas/users/user-1")
                        .with(authentication(adminAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","phone":"999999999"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"));
    }

    @Test
    void updateMe_returns200() throws Exception {
        when(userUseCase.update(any())).thenReturn(result(PRINCIPAL_ID.toString()));

        mockMvc.perform(patch("/saas/users/me")
                        .with(authentication(adminAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"updated@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void listAll_withSearchParam() throws Exception {
        when(userUseCase.listAll(eq("ana"), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(PaginatedResult.of(List.of(result("user-1")), 0, 20, 1));

        mockMvc.perform(get("/saas/users")
                        .param("search", "ana")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    private static String eq(String value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
