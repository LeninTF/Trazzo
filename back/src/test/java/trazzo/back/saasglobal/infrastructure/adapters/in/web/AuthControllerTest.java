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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    @MockitoBean trazzo.back.shared.security.TenantPermissionPort tenantPermissionPort;

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
    void login_tenantUserWithMissingTenant_failsInsteadOfIssuingClaimlessToken() throws Exception {
        // A tenant-scoped user (non-null tenantId) whose tenant/settings can't be resolved
        // must never silently receive a token without a tenant claim — JwtAuthFilter would
        // default that claim-less token to the "public" schema, breaking tenant isolation.
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "orphan.user@test.com", "pass", List.of(), true);
        var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        var user = User.restore(
                UUID.randomUUID().toString(), 1, "missing-tenant",
                "orphan.user@test.com", "999999999", "encodedPass",
                List.of("empleado"), List.of(), false, null, null, null
        );
        when(userRepository.findByEmail("orphan.user@test.com")).thenReturn(Optional.of(user));
        when(tenantRepository.findById("missing-tenant")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"orphan.user@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tenant schema not found for tenant user (tenantId=missing-tenant)"));
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

    @Test
    void login_tenantUserWithPermissions_injectsPermissionsInToken() throws Exception {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "perm.user@test.com", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_empleado")), true);
        var auth = new UsernamePasswordAuthenticationToken(authUser, null, authUser.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(any(AuthenticatedUser.class), eq("tenant_acme")))
                .thenReturn("jwt.token.here");

        var user = User.restore(
                UUID.randomUUID().toString(), 1, "t-1",
                "perm.user@test.com", "999999999", "encodedPass",
                List.of("empleado"), List.of(), false, null, null, null
        );
        when(userRepository.findByEmail("perm.user@test.com")).thenReturn(Optional.of(user));
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());

        var settings = TenantSettings.of("t-1", "tenant_acme");
        var tenant = Tenant.createTrial("acme", 1, null, settings, null);
        when(tenantRepository.findById("t-1")).thenReturn(Optional.of(tenant));

        when(tenantPermissionPort.findPermissionCodesByMasterUserId(any(UUID.class)))
                .thenReturn(List.of("incidents.crear", "incidents.aprobar"));

        var person = Person.restore(
                1, null, trazzo.back.saasglobal.domain.model.iam.DocumentType.DNI, "00000000",
                "Perm", "User", "Test",
                null, null, null
        );
        when(personRepository.findById(1)).thenReturn(Optional.of(person));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"perm.user@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.here"))
                .andExpect(jsonPath("$.usuario.tenant_permissions[0]").value("incidents.crear"))
                .andExpect(jsonPath("$.usuario.tenant_permissions[1]").value("incidents.aprobar"));
    }

    @Test
    void login_tenantUserWithEmptyPermissions_keepsOriginalPrincipal() throws Exception {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "empty.perm@tenant.com", "pass", List.of(), true);
        var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(authUser, "tenant_demo")).thenReturn("jwt.token.empty");

        var user = User.restore(
                UUID.randomUUID().toString(), 1, "t-2",
                "empty.perm@tenant.com", "999999999", "encodedPass",
                List.of("empleado"), List.of(), false, null, null, null
        );
        when(userRepository.findByEmail("empty.perm@tenant.com")).thenReturn(Optional.of(user));
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());

        var settings = TenantSettings.of("t-2", "tenant_demo");
        var tenant = Tenant.createTrial("demo", 1, null, settings, null);
        when(tenantRepository.findById("t-2")).thenReturn(Optional.of(tenant));

        when(tenantPermissionPort.findPermissionCodesByMasterUserId(any(UUID.class)))
                .thenReturn(List.of());

        var person = Person.restore(
                1, null, trazzo.back.saasglobal.domain.model.iam.DocumentType.DNI, "00000000",
                "Empty", "Perm", "Tenant",
                null, null, null
        );
        when(personRepository.findById(1)).thenReturn(Optional.of(person));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"empty.perm@tenant.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.empty"));
    }

    @Test
    void login_tenantUserWithInactiveStatus_returnsINACTIVO() throws Exception {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "inactive.user@test.com", "pass", List.of(), true);
        var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(authUser, "tenant_inactive")).thenReturn("jwt.inactive");

        var user = User.restore(
                UUID.randomUUID().toString(), 1, "t-inactive",
                "inactive.user@test.com", "999999999", "encodedPass",
                List.of("empleado"), List.of(), false, null, null, LocalDateTime.now()
        );
        when(userRepository.findByEmail("inactive.user@test.com")).thenReturn(Optional.of(user));
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());

        var settings = TenantSettings.of("t-inactive", "tenant_inactive");
        var tenant = Tenant.createTrial("inactive", 1, null, settings, null);
        when(tenantRepository.findById("t-inactive")).thenReturn(Optional.of(tenant));
        when(tenantPermissionPort.findPermissionCodesByMasterUserId(any(UUID.class)))
                .thenReturn(List.of());

        var person = Person.restore(
                1, null, trazzo.back.saasglobal.domain.model.iam.DocumentType.DNI, "00000000",
                "Inactive", "User", "Tenant",
                null, null, null
        );
        when(personRepository.findById(1)).thenReturn(Optional.of(person));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inactive.user@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.status").value("INACTIVO"));
    }

    @Test
    void login_succeeds_whenRoleRepositoryReturnsRoleMaster() throws Exception {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "role.found@test.com", "pass", List.of(), true);
        var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(authUser, null)).thenReturn("jwt.token");

        var user = User.restore(
                UUID.randomUUID().toString(), 1, null,
                "role.found@test.com", "999999999", "encodedPass",
                List.of("admin_trazzo"), List.of(), false, null, null, null
        );
        when(userRepository.findByEmail("role.found@test.com")).thenReturn(Optional.of(user));

        trazzo.back.saasglobal.domain.model.iam.RoleMaster role =
                trazzo.back.saasglobal.domain.model.iam.RoleMaster.restore(
                        1, "admin_trazzo", "Admin Trazzo", "Admin role", List.of("permission.one"));
        when(roleRepository.findByName("admin_trazzo")).thenReturn(Optional.of(role));

        var person = Person.restore(
                1, null, trazzo.back.saasglobal.domain.model.iam.DocumentType.DNI, "00000000",
                "Role", "Found", "User",
                null, null, null
        );
        when(personRepository.findById(1)).thenReturn(Optional.of(person));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"role.found@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol[0].id").value(1))
                .andExpect(jsonPath("$.usuario.rol[0].name").value("admin_trazzo"));
    }

    @Test
    void login_throws_whenPrincipalUserNotFoundInRepository() throws Exception {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "ghost@test.com", "pass", List.of(), true);
        var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_throws_whenPersonNotFoundForAuthenticatedUser() throws Exception {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "no.person@test.com", "pass", List.of(), true);
        var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        var user = User.restore(
                UUID.randomUUID().toString(), 1, null,
                "no.person@test.com", "999999999", "encodedPass",
                List.of("admin_trazzo"), List.of(), false, null, null, null
        );
        when(userRepository.findByEmail("no.person@test.com")).thenReturn(Optional.of(user));
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(personRepository.findById(1)).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no.person@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "anonymous@test.com")
    void me_returns200_withAuthenticatedAuthorityList() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("anonymous@test.com"));
    }

}
