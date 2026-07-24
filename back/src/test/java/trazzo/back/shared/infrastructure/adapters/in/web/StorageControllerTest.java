package trazzo.back.shared.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.shared.application.port.out.FileStoragePort;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.SecurityTestMockConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@WebMvcTest(StorageController.class)
@Import(SecurityTestMockConfig.class)
@EnableMethodSecurity
class StorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStoragePort fileStoragePort;

    @MockitoBean
    private TenantUserPort tenantUserPort;

    private AuthenticatedUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AuthenticatedUser(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "test@mail.com", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("incidencias.crear")), true);
        var auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPresignedUrlShouldReturnUrlAndObjectKey() throws Exception {
        when(tenantUserPort.findIdByMasterUserId(testUser.id())).thenReturn(Optional.of(42L));
        when(fileStoragePort.generatePresignedPutUrl(anyString(), anyString(), any()))
                .thenReturn("https://r2.example.com/presigned");

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "photo.jpg")
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presigned_url").value("https://r2.example.com/presigned"))
                .andExpect(jsonPath("$.object_key").isNotEmpty())
                .andExpect(jsonPath("$.object_key").value(org.hamcrest.Matchers.containsString("photo.jpg")));
    }

    @Test
    void getPresignedUrlShouldIncludeTenantIdInObjectKey() throws Exception {
        when(tenantUserPort.findIdByMasterUserId(testUser.id())).thenReturn(Optional.of(42L));
        when(fileStoragePort.generatePresignedPutUrl(anyString(), anyString(), any()))
                .thenReturn("https://r2.example.com/presigned");

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "doc.pdf")
                        .param("contentType", "application/pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object_key").value(org.hamcrest.Matchers.startsWith("evidences/42/")))
                .andExpect(jsonPath("$.object_key").value(org.hamcrest.Matchers.containsString("doc.pdf")));
    }

    @Test
    void getPresignedUrlShouldIncludeIncidentIdInObjectKeyWhenProvided() throws Exception {
        when(tenantUserPort.findIdByMasterUserId(testUser.id())).thenReturn(Optional.of(42L));
        when(fileStoragePort.generatePresignedPutUrl(anyString(), anyString(), any()))
                .thenReturn("https://r2.example.com/presigned");

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "doc.pdf")
                        .param("contentType", "application/pdf")
                        .param("incident_id", "77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object_key").value(org.hamcrest.Matchers.containsString("evidences/42/77/")));
    }

    @Test
    void getPresignedUrlShouldReturn403WithoutPermission() throws Exception {
        var restrictedUser = new AuthenticatedUser(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
                "restricted@mail.com", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")), true);
        var restrictedAuth = new UsernamePasswordAuthenticationToken(restrictedUser, null, restrictedUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(restrictedAuth);

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "doc.pdf")
                        .param("contentType", "application/pdf"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void getPresignedUrlShouldFallbackToUnknownTenantWhenUserHasNoTenantMapping() throws Exception {
        when(tenantUserPort.findIdByMasterUserId(testUser.id())).thenReturn(Optional.empty());
        when(fileStoragePort.generatePresignedPutUrl(anyString(), anyString(), any()))
                .thenReturn("https://r2.example.com/presigned");

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "doc.pdf")
                        .param("contentType", "application/pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object_key").value(org.hamcrest.Matchers.startsWith("evidences/unknown-tenant/")));
    }

    @Test
    void getPresignedUrlShouldReturn400WhenMissingFileName() throws Exception {
        mockMvc.perform(get("/storage/presigned-url")
                        .param("contentType", "application/pdf"))
                .andExpect(status().isBadRequest());
    }
}
