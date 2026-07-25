package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.UserBiometriaResult;
import trazzo.back.corehr.application.port.in.UserBiometriaUseCase;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollService;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollSessionResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserBiometriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserBiometriaControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserBiometriaUseCase userBiometriaUseCase;

    @MockitoBean
    EnrollService enrollService;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private static UserBiometriaResult aBioResult() {
        return new UserBiometriaResult(1L, 10L, 5L, "DVC-001", 2, true, NOW, NOW, NOW);
    }

    private static EnrollSessionResponse aSessionResponse() {
        return new EnrollSessionResponse("token-abc", 10L, 5L, 2, "DVC-001", NOW.plusSeconds(120));
    }

    @Test
    void list_shouldReturn200() throws Exception {
        var paginated = new PaginatedResult<UserBiometriaResult>(List.of(aBioResult()), 0, 20, 1, 1);
        when(userBiometriaUseCase.findAll(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(paginated);

        mockMvc.perform(get("/corehr/biometria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].finger_index").value(2))
                .andExpect(jsonPath("$.content[0].activo").value(true))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void list_shouldFilterByQueryParams() throws Exception {
        var paginated = new PaginatedResult<UserBiometriaResult>(List.of(aBioResult()), 0, 5, 1, 1);
        when(userBiometriaUseCase.findAll(eq(10L), eq(5L), eq(true), eq(0), eq(5)))
                .thenReturn(paginated);

        mockMvc.perform(get("/corehr/biometria")
                        .param("tenant_user_id", "10")
                        .param("device_id", "5")
                        .param("activo", "true")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void initEnroll_shouldReturn201() throws Exception {
        when(enrollService.initEnroll(anyLong(), anyLong(), anyInt())).thenReturn(aSessionResponse());

        mockMvc.perform(post("/corehr/biometria/enroll/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenant_user_id": 10, "device_id": 5, "finger_index": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enroll_token").value("token-abc"))
                .andExpect(jsonPath("$.finger_index").value(2));
    }

    @Test
    void initEnroll_shouldReturn400WhenTenantUserIdIsNull() throws Exception {
        mockMvc.perform(post("/corehr/biometria/enroll/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"device_id": 5, "finger_index": 2}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void initEnroll_shouldReturn400WhenFingerIndexIsNegative() throws Exception {
        mockMvc.perform(post("/corehr/biometria/enroll/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenant_user_id": 10, "device_id": 5, "finger_index": -1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pendingEnroll_shouldReturn200WhenFound() throws Exception {
        var pendingResponse = new trazzo.back.corehr.infrastructure.adapters.in.web.dto.PendingEnrollSessionResponse(
                "token-abc", 10L, "DVC-001", 5L, 2, NOW.plusSeconds(120));
        when(userBiometriaUseCase.findPendingEnrollSession("DVC-001"))
                .thenReturn(Optional.of(
                        new trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollSession("token-abc", 5L, 10L, 2, "DVC-001", NOW.plusSeconds(120))));

        mockMvc.perform(get("/corehr/biometria/enroll/pendiente")
                        .param("device_code", "DVC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enroll_token").value("token-abc"))
                .andExpect(jsonPath("$.device_code").value("DVC-001"));
    }

    @Test
    void pendingEnroll_shouldReturn404WhenNotFound() throws Exception {
        when(userBiometriaUseCase.findPendingEnrollSession("UNKNOWN"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/corehr/biometria/enroll/pendiente")
                        .param("device_code", "UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void completeEnroll_shouldReturn201() throws Exception {
        when(enrollService.completeEnroll(anyString(), anyString(), anyInt(),
                anyString(), anyString(), anyString(), anyString(), any())).thenReturn(aBioResult());

        mockMvc.perform(post("/corehr/biometria/enroll/completar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enroll_token": "tok", "encrypted_template_base64": "dGVtcA==", "encrypted_aes_key_base64": "a2V5", "iv_base64": "aXY=", "tag_base64": "dGFn", "finger_index": 2, "device_code": "DVC-001"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.finger_index").value(2));
    }

    @Test
    void completeEnroll_shouldReturn400WhenEnrollTokenIsBlank() throws Exception {
        mockMvc.perform(post("/corehr/biometria/enroll/completar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enroll_token": "", "encrypted_template_base64": "dGVtcA==", "encrypted_aes_key_base64": "a2V5", "finger_index": 2, "device_code": "DVC-001"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeEnroll_shouldReturn400WhenTemplateIsBlank() throws Exception {
        mockMvc.perform(post("/corehr/biometria/enroll/completar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enroll_token": "tok", "encrypted_template_base64": "", "encrypted_aes_key_base64": "a2V5", "finger_index": 2, "device_code": "DVC-001"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchActivo_shouldReturn200() throws Exception {
        when(userBiometriaUseCase.patchActivo(1L, false)).thenReturn(aBioResult());

        mockMvc.perform(patch("/corehr/biometria/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activo": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void patchActivo_shouldReturn400WhenActivoIsNull() throws Exception {
        mockMvc.perform(patch("/corehr/biometria/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }
}
