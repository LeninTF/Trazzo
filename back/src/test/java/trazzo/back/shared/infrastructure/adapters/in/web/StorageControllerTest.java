package trazzo.back.shared.infrastructure.adapters.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import trazzo.back.shared.security.TenantSecuritySlice;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.incidents.application.dto.result.IncidentResult;
import trazzo.back.incidents.application.port.in.IncidentUseCase;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.shared.application.port.out.FileStoragePort;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.SecurityTestMockConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@WebMvcTest(StorageController.class)
@Import(SecurityTestMockConfig.class)
@TenantSecuritySlice
class StorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStoragePort fileStoragePort;

    @MockitoBean
    private TenantUserPort tenantUserPort;

    @MockitoBean
    private IncidentUseCase incidentUseCase;

    private AuthenticatedUser testUser;

    @BeforeEach
    void setUp() {
        testUser = actingAsUserWith("incidencias.crear");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static AuthenticatedUser actingAsUserWith(String... permissions) {
        var authorities = new java.util.ArrayList<>(List.of(new SimpleGrantedAuthority("ROLE_USER")));
        java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).forEach(authorities::add);
        var user = new AuthenticatedUser(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "test@mail.com", "pass", authorities, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return user;
    }

    private static IncidentResult anIncidentOf(int tenantUserId) {
        var now = LocalDateTime.now();
        return new IncidentResult(77, tenantUserId, 1, IncidentState.PENDIENTE, "comment", null, null, null, List.of(),
                null, now, now);
    }

    // Usuario 42 con su propia incidencia 77.
    private void anOwnIncident() {
        when(tenantUserPort.findIdByMasterUserId(testUser.id())).thenReturn(Optional.of(42L));
        when(incidentUseCase.findById(77)).thenReturn(Optional.of(anIncidentOf(42)));
        when(fileStoragePort.generatePresignedPutUrl(anyString(), anyString(), any()))
                .thenReturn("https://r2.example.com/presigned");
    }

    @Test
    void getPresignedUrlShouldReturnUrlAndObjectKey() throws Exception {
        anOwnIncident();

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "photo.jpg")
                        .param("contentType", "image/jpeg")
                        .param("incident_id", "77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presigned_url").value("https://r2.example.com/presigned"))
                .andExpect(jsonPath("$.object_key").isNotEmpty())
                .andExpect(jsonPath("$.object_key").value(containsString("photo.jpg")));
    }

    @Test
    void getPresignedUrlShouldIncludeTenantUserAndIncidentInObjectKey() throws Exception {
        anOwnIncident();

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "doc.pdf")
                        .param("contentType", "application/pdf")
                        .param("incident_id", "77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object_key").value(startsWith("evidences/42/77/")))
                .andExpect(jsonPath("$.object_key").value(endsWith("/doc.pdf")));
    }

    @Test
    void aPathInTheFileNameIsReducedToTheName() throws Exception {
        anOwnIncident();

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "../../x.pdf")
                        .param("contentType", "application/pdf")
                        .param("incident_id", "77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object_key").value(startsWith("evidences/42/77/")))
                .andExpect(jsonPath("$.object_key").value(endsWith("/x.pdf")))
                .andExpect(jsonPath("$.object_key").value(not(containsString(".."))));
        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "C:\\fakepath\\..\\informe.pdf")
                        .param("contentType", "application/pdf")
                        .param("incident_id", "77"))
                .andExpect(jsonPath("$.object_key").value(endsWith("/informe.pdf")));
    }

    @Test
    void aFileNameWithoutAnActualNameIs400() throws Exception {
        anOwnIncident();

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "../")
                        .param("contentType", "application/pdf")
                        .param("incident_id", "77"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void aMimeTypeOutsideTheEvidenceWhitelistIs400() throws Exception {
        anOwnIncident();

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "setup.exe")
                        .param("contentType", "application/x-msdownload")
                        .param("incident_id", "77"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void theIncidentIsRequired() throws Exception {
        anOwnIncident();

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "doc.pdf")
                        .param("contentType", "application/pdf"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void anUnknownIncidentIs404() throws Exception {
        anOwnIncident();
        when(incidentUseCase.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "doc.pdf")
                        .param("contentType", "application/pdf")
                        .param("incident_id", "99"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void someoneElsesIncidentIs403WithoutApproval() throws Exception {
        anOwnIncident();
        when(incidentUseCase.findById(77)).thenReturn(Optional.of(anIncidentOf(7)));

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "doc.pdf")
                        .param("contentType", "application/pdf")
                        .param("incident_id", "77"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void anApproverGetsAnUploadUrlForAnyIncident() throws Exception {
        testUser = actingAsUserWith("incidencias.crear", "incidencias.aprobar-rechazar");
        anOwnIncident();
        when(incidentUseCase.findById(77)).thenReturn(Optional.of(anIncidentOf(7)));

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "doc.pdf")
                        .param("contentType", "application/pdf")
                        .param("incident_id", "77"))
                .andExpect(status().isOk());
    }

    @Test
    void getPresignedUrlShouldReturn403WithoutPermission() throws Exception {
        actingAsUserWith();

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "doc.pdf")
                        .param("contentType", "application/pdf")
                        .param("incident_id", "77"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void aUserWithoutATenantProfileGetsNoUploadUrl() throws Exception {
        anOwnIncident();
        when(tenantUserPort.findIdByMasterUserId(testUser.id())).thenReturn(Optional.empty());

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "doc.pdf")
                        .param("contentType", "application/pdf")
                        .param("incident_id", "77"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void getPresignedUrlShouldReturn400WhenMissingFileName() throws Exception {
        mockMvc.perform(get("/storage/presigned-url")
                        .param("contentType", "application/pdf")
                        .param("incident_id", "77"))
                .andExpect(status().isBadRequest());
    }
}
