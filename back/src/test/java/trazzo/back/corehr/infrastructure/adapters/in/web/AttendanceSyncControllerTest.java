package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import trazzo.back.shared.security.DeviceAgentPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.result.AttendanceResult;
import trazzo.back.corehr.application.dto.result.SyncItemResult;
import trazzo.back.corehr.application.usecase.MarkAttendanceUseCase;
import trazzo.back.corehr.application.usecase.SyncAttendanceUseCase;
import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceSyncController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttendanceSyncControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MarkAttendanceUseCase markAttendanceUseCase;

    @MockitoBean
    SyncAttendanceUseCase syncAttendanceUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDate TODAY = NOW.toLocalDate();

    private static AttendanceResult anAttendanceResult() {
        return new AttendanceResult(
                "att-001", 100L, null, null, null, 1L, "DVC-001",
                NOW, null, TODAY, 5, AttendanceState.TARDANZA, NOW, NOW
        );
    }

    private static String validRequestJson() {
        return """
                {
                    "event_type": "check_in",
                    "encrypted_template_base64": "dGVtcA==",
                    "encrypted_aes_key_base64": "a2V5",
                    "iv_base64": "aXY=",
                    "tag_base64": "dGFn",
                    "captured_at_utc": "2025-01-15T08:30:00",
                    "device_code": "DVC-001",
                    "tenant_user_id": 100
                }
                """;
    }


    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // El filtro del token de dispositivo deja este principal; aquí se simula sin pasar por él.
    private static void actingAsAgentOf(String deviceCode) {
        var agent = new DeviceAgentPrincipal(UUID.randomUUID(), "tenant_demo", 7L, deviceCode);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                agent, null, List.of(new SimpleGrantedAuthority("ROLE_AGENT"))));
    }

    @Test
    void mark_anAgentCannotMarkForAnotherDevice() throws Exception {
        actingAsAgentOf("DVC-OTRO");

        mockMvc.perform(post("/asistencia/marcar").contentType(MediaType.APPLICATION_JSON).content(validRequestJson()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(markAttendanceUseCase);
    }

    @Test
    void mark_anAgentMarksForItsOwnDevice() throws Exception {
        actingAsAgentOf("DVC-001");
        when(markAttendanceUseCase.mark(any())).thenReturn(anAttendanceResult());

        mockMvc.perform(post("/asistencia/marcar").contentType(MediaType.APPLICATION_JSON).content(validRequestJson()))
                .andExpect(status().isOk());
    }

    @Test
    void sync_anAgentCannotSyncAnotherDevicesEvents() throws Exception {
        actingAsAgentOf("DVC-OTRO");

        mockMvc.perform(post("/asistencia/sync").contentType(MediaType.APPLICATION_JSON).content("[" + validRequestJson() + "]"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(syncAttendanceUseCase);
    }

    @Test
    void mark_shouldReturn200() throws Exception {
        when(markAttendanceUseCase.mark(any())).thenReturn(anAttendanceResult());

        mockMvc.perform(post("/asistencia/marcar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("att-001"))
                .andExpect(jsonPath("$.tenant_user_id").value(100))
                .andExpect(jsonPath("$.device_code").value("DVC-001"))
                .andExpect(jsonPath("$.minutes_late").value(5))
                .andExpect(jsonPath("$.state").value("TARDANZA"));
    }

    @Test
    void mark_shouldReturn400WhenTemplateIsBlank() throws Exception {
        mockMvc.perform(post("/asistencia/marcar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "encrypted_template_base64": "",
                                    "encrypted_aes_key_base64": "a2V5",
                                    "iv_base64": "aXY=",
                                    "tag_base64": "dGFn",
                                    "captured_at_utc": "2025-01-15T08:30:00",
                                    "device_code": "DVC-001"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mark_shouldReturn400WhenCapturedAtUtcIsNull() throws Exception {
        mockMvc.perform(post("/asistencia/marcar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "encrypted_template_base64": "dGVtcA==",
                                    "encrypted_aes_key_base64": "a2V5",
                                    "iv_base64": "aXY=",
                                    "tag_base64": "dGFn",
                                    "device_code": "DVC-001"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mark_shouldReturn400WhenDeviceCodeIsBlank() throws Exception {
        mockMvc.perform(post("/asistencia/marcar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "encrypted_template_base64": "dGVtcA==",
                                    "encrypted_aes_key_base64": "a2V5",
                                    "iv_base64": "aXY=",
                                    "tag_base64": "dGFn",
                                    "captured_at_utc": "2025-01-15T08:30:00",
                                    "device_code": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sync_shouldReturn202() throws Exception {
        var correlationId = UUID.randomUUID();
        var batchResult = new SyncAttendanceUseCase.SyncBatchResult(
                correlationId, 2,
                List.of(
                        SyncItemResult.success("att-001", AttendanceState.PUNTUAL, 0, NOW, 1001),
                        SyncItemResult.success("att-002", AttendanceState.TARDANZA, 10, NOW, 1002)
                ));
        when(syncAttendanceUseCase.syncBatch(any(), any())).thenReturn(batchResult);

        mockMvc.perform(post("/asistencia/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {
                                        "encrypted_template_base64": "dGVtcA==",
                                        "encrypted_aes_key_base64": "a2V5",
                                        "iv_base64": "aXY=",
                                        "tag_base64": "dGFn",
                                        "captured_at_utc": "2025-01-15T08:30:00",
                                        "device_code": "DVC-001",
                                        "tenant_user_id": 100,
                                        "offline_event_id": 1001
                                    },
                                    {
                                        "encrypted_template_base64": "dGVtcA==",
                                        "encrypted_aes_key_base64": "a2V5",
                                        "iv_base64": "aXY=",
                                        "tag_base64": "dGFn",
                                        "captured_at_utc": "2025-01-15T18:00:00",
                                        "device_code": "DVC-001",
                                        "tenant_user_id": 100,
                                        "offline_event_id": 1002
                                    }
                                ]
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Lote aceptado para procesamiento asincrónico."))
                .andExpect(jsonPath("$.accepted_count").value(2))
                .andExpect(jsonPath("$.correlation_id").isNotEmpty());
    }

    // El agente envía un evento por lote: con 202 lo da por enviado, así que un rechazo tiene que ser un 4xx.
    @Test
    void sync_aBatchWhereNothingWasAcceptedNorSkippedIs422WithItems() throws Exception {
        when(syncAttendanceUseCase.syncBatch(any(), any())).thenReturn(new SyncAttendanceUseCase.SyncBatchResult(
                UUID.randomUUID(), 0, List.of(SyncItemResult.failure("Huella no reconocida", 1001))));

        mockMvc.perform(post("/asistencia/sync").contentType(MediaType.APPLICATION_JSON).content("[" + validRequestJson() + "]"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.accepted_count").value(0))
                .andExpect(jsonPath("$.items[0].offline_event_id").value(1001))
                .andExpect(jsonPath("$.items[0].status").value("failed"))
                .andExpect(jsonPath("$.items[0].message").value("Huella no reconocida"));
    }

    @Test
    void sync_aMixedBatchIs202AndReportsEveryItem() throws Exception {
        when(syncAttendanceUseCase.syncBatch(any(), any())).thenReturn(new SyncAttendanceUseCase.SyncBatchResult(
                UUID.randomUUID(), 1, List.of(
                        SyncItemResult.success("att-001", AttendanceState.PUNTUAL, 0, NOW, 1001),
                        SyncItemResult.failure("Huella no reconocida", 1002),
                        SyncItemResult.skipped(1003))));

        mockMvc.perform(post("/asistencia/sync").contentType(MediaType.APPLICATION_JSON).content("[" + validRequestJson() + "]"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accepted_count").value(1))
                .andExpect(jsonPath("$.items[0].offline_event_id").value(1001))
                .andExpect(jsonPath("$.items[0].status").value("accepted"))
                .andExpect(jsonPath("$.items[1].status").value("failed"))
                .andExpect(jsonPath("$.items[1].message").value("Huella no reconocida"))
                .andExpect(jsonPath("$.items[2].offline_event_id").value(1003))
                .andExpect(jsonPath("$.items[2].status").value("skipped"));
    }

    // Un reenvío de algo ya procesado no es un error: el agente debe darlo por enviado.
    @Test
    void sync_aBatchOfAlreadyProcessedEventsIs202() throws Exception {
        when(syncAttendanceUseCase.syncBatch(any(), any())).thenReturn(new SyncAttendanceUseCase.SyncBatchResult(
                UUID.randomUUID(), 0, List.of(SyncItemResult.skipped(1001))));

        mockMvc.perform(post("/asistencia/sync").contentType(MediaType.APPLICATION_JSON).content("[" + validRequestJson() + "]"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.items[0].status").value("skipped"));
    }

    @Test
    void sync_shouldReturn202WithEmptyList() throws Exception {
        var correlationId = UUID.randomUUID();
        var batchResult = new SyncAttendanceUseCase.SyncBatchResult(
                correlationId, 0, List.of());
        when(syncAttendanceUseCase.syncBatch(any(), any())).thenReturn(batchResult);

        mockMvc.perform(post("/asistencia/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accepted_count").value(0));
    }

    @Test
    void sync_shouldExtractTenantUserIdFromFirstRequest() throws Exception {
        var correlationId = UUID.randomUUID();
        var batchResult = new SyncAttendanceUseCase.SyncBatchResult(
                correlationId, 1,
                List.of(SyncItemResult.success("att-001", AttendanceState.PUNTUAL, 0, NOW, 1001)));
        when(syncAttendanceUseCase.syncBatch(any(), any())).thenReturn(batchResult);

        mockMvc.perform(post("/asistencia/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {
                                        "encrypted_template_base64": "dGVtcA==",
                                        "encrypted_aes_key_base64": "a2V5",
                                        "iv_base64": "aXY=",
                                        "tag_base64": "dGFn",
                                        "captured_at_utc": "2025-01-15T08:30:00",
                                        "device_code": "DVC-001",
                                        "tenant_user_id": 200
                                    }
                                ]
                                """))
                .andExpect(status().isAccepted());
    }
}
