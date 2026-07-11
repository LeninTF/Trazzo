package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.port.out.PersonRepositoryPort;
import trazzo.back.saasglobal.application.port.out.RoleMasterRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.Person;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;
import trazzo.back.shared.security.JwtService;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.SecurityConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean JwtService jwtService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepositoryPort userRepository;
    @MockitoBean PersonRepositoryPort personRepository;
    @MockitoBean TenantRepositoryPort tenantRepository;
    @MockitoBean RoleMasterRepositoryPort roleRepository;

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void me_returns200WithEmailAndRoles() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "user@test.com", "pass", List.of(), true);
        var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(authUser, null)).thenReturn("jwt.token.here");

        var user = User.restore(
                UUID.randomUUID().toString(), 1, null,
                "user@test.com", "999999999", "encodedPass",
                List.of("admin_trazzo"), List.of(), false, null, null, null
        );
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());

        var person = Person.restore(
                1, null, trazzo.back.saasglobal.domain.model.iam.DocumentType.DNI, "00000000",
                "Admin", "Trazzo", "Sistema",
                null, null, null
        );
        when(personRepository.findById(1)).thenReturn(Optional.of(person));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.here"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.nombre").value("Admin"))
                .andExpect(jsonPath("$.usuario.apellido_paterno").value("Trazzo"))
                .andExpect(jsonPath("$.usuario.apellido_materno").value("Sistema"))
                .andExpect(jsonPath("$.usuario.email").value("user@test.com"))
                .andExpect(jsonPath("$.usuario.status").value("ACTIVO"))
                .andExpect(jsonPath("$.usuario.rol[0].name").value("admin_trazzo"));
    }

    @Test
    void login_tenantUser_includesTenantSchemaInToken() throws Exception {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "tenant.user@test.com", "pass", List.of(), true);
        var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(authUser, "tenant_acme")).thenReturn("jwt.token.here");

        var user = User.restore(
                UUID.randomUUID().toString(), 1, "t-1",
                "tenant.user@test.com", "999999999", "encodedPass",
                List.of("empleado"), List.of(), false, null, null, null
        );
        when(userRepository.findByEmail("tenant.user@test.com")).thenReturn(Optional.of(user));
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());

        var settings = TenantSettings.of("t-1", "tenant_acme");
        var tenant = Tenant.createTrial("acme", 1, null, settings, null);
        when(tenantRepository.findById("t-1")).thenReturn(Optional.of(tenant));

        var person = Person.restore(
                1, null, trazzo.back.saasglobal.domain.model.iam.DocumentType.DNI, "00000000",
                "Tenant", "User", "Test",
                null, null, null
        );
        when(personRepository.findById(1)).thenReturn(Optional.of(person));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tenant.user@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.here"));
    }

    @Test
    void login_tenantNotFound_generatesTokenWithoutTenantClaim() throws Exception {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "orphan.user@test.com", "pass", List.of(), true);
        var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(authUser, null)).thenReturn("jwt.token.here");

        var user = User.restore(
                UUID.randomUUID().toString(), 1, "missing-tenant",
                "orphan.user@test.com", "999999999", "encodedPass",
                List.of("empleado"), List.of(), false, null, null, null
        );
        when(userRepository.findByEmail("orphan.user@test.com")).thenReturn(Optional.of(user));
        when(tenantRepository.findById("missing-tenant")).thenReturn(Optional.empty());
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());

        var person = Person.restore(
                1, null, trazzo.back.saasglobal.domain.model.iam.DocumentType.DNI, "00000000",
                "Orphan", "User", "Test",
                null, null, null
        );
        when(personRepository.findById(1)).thenReturn(Optional.of(person));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"orphan.user@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.here"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad@test.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

}
