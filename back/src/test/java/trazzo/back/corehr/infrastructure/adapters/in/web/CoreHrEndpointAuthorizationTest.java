package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.TenantUserProfileResult;
import trazzo.back.corehr.application.port.in.AttendanceUseCase;
import trazzo.back.corehr.application.port.in.DeviceUseCase;
import trazzo.back.corehr.application.port.in.NonWorkingDayUseCase;
import trazzo.back.corehr.application.port.in.ScheduleUseCase;
import trazzo.back.corehr.application.port.in.ShiftUseCase;
import trazzo.back.corehr.application.port.in.TenantContactUseCase;
import trazzo.back.corehr.application.port.in.TenantUserDepartmentUseCase;
import trazzo.back.corehr.application.port.in.TenantUserUseCase;
import trazzo.back.corehr.application.port.in.ToleranciaUseCase;
import trazzo.back.corehr.application.port.in.UserBiometriaUseCase;
import trazzo.back.corehr.application.port.in.UserScheduleUseCase;
import trazzo.back.corehr.application.usecase.DeviceAgentTokenService;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollService;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollSessionResponse;
import trazzo.back.shared.security.TenantSecuritySlice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static trazzo.back.shared.security.TenantSecurityContexts.MASTER_USER_ID;
import static trazzo.back.shared.security.TenantSecurityContexts.actingAsPlainUser;
import static trazzo.back.shared.security.TenantSecurityContexts.actingAsTenantUserWith;

/** Matriz permiso → endpoint de corehr (AUD-05): el rol "usuario" no administra; cada permiso abre solo lo suyo. */
@WebMvcTest({AttendanceController.class, DeviceController.class, NonWorkingDayController.class, ScheduleController.class,
        ShiftController.class, TenantContactController.class, TenantUserController.class,
        TenantUserDepartmentController.class, ToleranciaController.class, UserBiometriaController.class,
        UserScheduleController.class})
@AutoConfigureMockMvc(addFilters = false)
@TenantSecuritySlice
class CoreHrEndpointAuthorizationTest {

    private static final String ATTENDANCE_ID = "3f2b8c1e-4d5a-4b6c-8d7e-9f0a1b2c3d4e";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AttendanceUseCase attendanceUseCase;
    @MockitoBean
    DeviceUseCase deviceUseCase;
    @MockitoBean
    DeviceAgentTokenService deviceAgentTokenService;
    @MockitoBean
    NonWorkingDayUseCase nonWorkingDayUseCase;
    @MockitoBean
    ScheduleUseCase scheduleUseCase;
    @MockitoBean
    ShiftUseCase shiftUseCase;
    @MockitoBean
    TenantContactUseCase tenantContactUseCase;
    @MockitoBean
    TenantUserUseCase tenantUserUseCase;
    @MockitoBean
    TenantUserDepartmentUseCase tenantUserDepartmentUseCase;
    @MockitoBean
    ToleranciaUseCase toleranciaUseCase;
    @MockitoBean
    UserBiometriaUseCase userBiometriaUseCase;
    @MockitoBean
    EnrollService enrollService;
    @MockitoBean
    UserScheduleUseCase userScheduleUseCase;

    private static <T> PaginatedResult<T> emptyPage() {
        return new PaginatedResult<>(List.of(), 0, 20, 0, 0);
    }

    private static TenantUserProfileResult aProfile() {
        return new TenantUserProfileResult(1L, "usuario@demo.trazzo.pe", null, "ACTIVO", false, null, null, null, null,
                List.of(), List.of(), List.of());
    }

    @Test
    void thePlainUserCannotListOrManageStaff() throws Exception {
        actingAsPlainUser();

        mockMvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios/1")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/usuarios/1")).andExpect(status().isForbidden());
        verifyNoInteractions(tenantUserUseCase);
    }

    @Test
    void thePlainUserReadsAndUpdatesTheirOwnProfile() throws Exception {
        actingAsPlainUser();
        when(tenantUserUseCase.findMe(MASTER_USER_ID.toString())).thenReturn(Optional.of(aProfile()));

        mockMvc.perform(get("/usuarios/me")).andExpect(status().isOk());
        mockMvc.perform(patch("/usuarios/me").contentType(MediaType.APPLICATION_JSON).content("{\"phone\": \"999888777\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void removingStaffNeedsTheDeletePermissionNotJustEdit() throws Exception {
        actingAsTenantUserWith("gestion-trabajadores.editar");
        mockMvc.perform(delete("/usuarios/1")).andExpect(status().isForbidden());

        actingAsTenantUserWith("gestion-trabajadores.eliminar");
        mockMvc.perform(delete("/usuarios/1")).andExpect(status().isOk());
        verify(tenantUserUseCase).delete(1L);
    }

    @Test
    void massScheduleAssignmentIsEnoughToSeeTheStaff() throws Exception {
        actingAsTenantUserWith("gestion-horarios.asignacion-masiva");

        mockMvc.perform(get("/usuarios")).andExpect(status().isOk());
    }

    @Test
    void thePlainUserCannotSeeOrChangeShiftsSchedulesTolerancesOrAssignments() throws Exception {
        actingAsPlainUser();

        mockMvc.perform(get("/corehr/shifts")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/corehr/shifts/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/corehr/schedules")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/corehr/schedules/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/corehr/schedules/1/tolerancias")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/corehr/schedules/1/tolerancias/2")).andExpect(status().isForbidden());
        mockMvc.perform(get("/corehr/user-schedules")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/corehr/user-schedules/1")).andExpect(status().isForbidden());
        verifyNoInteractions(shiftUseCase, scheduleUseCase, toleranciaUseCase, userScheduleUseCase);
    }

    @Test
    void shiftConfigurationDoesNotOpenTolerancesNorAssignments() throws Exception {
        actingAsTenantUserWith("gestion-horarios.configurar-turnos");

        mockMvc.perform(delete("/corehr/shifts/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/corehr/schedules/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/corehr/non-working-days/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/corehr/schedules/1/tolerancias/2")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/corehr/user-schedules/1")).andExpect(status().isForbidden());
    }

    @Test
    void tolerancesAndAssignmentsHaveTheirOwnPermissions() throws Exception {
        actingAsTenantUserWith("gestion-horarios.configurar-tolerancias");
        mockMvc.perform(delete("/corehr/schedules/1/tolerancias/2")).andExpect(status().isNoContent());

        actingAsTenantUserWith("gestion-horarios.asignacion-masiva");
        mockMvc.perform(delete("/corehr/user-schedules/1")).andExpect(status().isNoContent());
    }

    @Test
    void teacherScheduleReadersSeeShiftsButCannotChangeThem() throws Exception {
        actingAsTenantUserWith("horarios-docente.ver-turnos");
        when(shiftUseCase.findAll(any(), anyInt(), anyInt(), any())).thenReturn(emptyPage());

        mockMvc.perform(get("/corehr/shifts")).andExpect(status().isOk());
        mockMvc.perform(delete("/corehr/shifts/1")).andExpect(status().isForbidden());
    }

    @Test
    void everyoneSeesNonWorkingDaysButThePlainUserCannotChangeThem() throws Exception {
        actingAsPlainUser();
        when(nonWorkingDayUseCase.findAll(any(), any(), any(), anyInt(), anyInt())).thenReturn(emptyPage());

        mockMvc.perform(get("/corehr/non-working-days")).andExpect(status().isOk());
        mockMvc.perform(delete("/corehr/non-working-days/1")).andExpect(status().isForbidden());
    }

    @Test
    void thePlainUserCannotSeeDevicesNorBiometrics() throws Exception {
        actingAsPlainUser();

        mockMvc.perform(get("/corehr/devices")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/corehr/devices/1")).andExpect(status().isForbidden());
        mockMvc.perform(post("/corehr/devices/1/agent-token")).andExpect(status().isForbidden());
        mockMvc.perform(get("/corehr/biometria")).andExpect(status().isForbidden());
        mockMvc.perform(post("/corehr/biometria/enroll/iniciar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenant_user_id\": 10, \"device_id\": 5, \"finger_index\": 2}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(deviceUseCase, deviceAgentTokenService, userBiometriaUseCase, enrollService);
    }

    @Test
    void theAttendanceOperatorSeesDevicesButOnlyTheMethodsManagerChangesThem() throws Exception {
        actingAsTenantUserWith("control-asistencia.operador-asistencia");
        when(deviceUseCase.findAll(any(), any(), anyInt(), anyInt())).thenReturn(emptyPage());
        mockMvc.perform(get("/corehr/devices")).andExpect(status().isOk());
        mockMvc.perform(delete("/corehr/devices/1")).andExpect(status().isForbidden());

        actingAsTenantUserWith("configuracion-tenant.gestionar-metodos");
        mockMvc.perform(delete("/corehr/devices/1")).andExpect(status().isNoContent());
    }

    @Test
    void aStaffEditorCanStartAnEnrollment() throws Exception {
        actingAsTenantUserWith("gestion-trabajadores.editar");
        when(enrollService.initEnroll(anyLong(), anyLong(), anyInt())).thenReturn(
                new EnrollSessionResponse("token-abc", 10L, 5L, 2, "DVC-001", LocalDateTime.now().plusSeconds(120)));

        mockMvc.perform(post("/corehr/biometria/enroll/iniciar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenant_user_id\": 10, \"device_id\": 5, \"finger_index\": 2}"))
                .andExpect(status().isCreated());
    }

    @Test
    void thePlainUserCannotCorrectAttendance() throws Exception {
        actingAsPlainUser();

        mockMvc.perform(patch("/corehr/attendance/" + ATTENDANCE_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minutes_late\": 0}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(attendanceUseCase);
    }

    @Test
    void thePlainUserCannotSeeContactsNorDepartmentAssignments() throws Exception {
        actingAsPlainUser();

        mockMvc.perform(get("/corehr/tenant-contacts")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/corehr/tenant-contacts/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/corehr/usuarios/1/departamentos")).andExpect(status().isForbidden());
        mockMvc.perform(post("/corehr/usuarios/1/departamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department_id\": 3, \"is_primary\": true, \"start_date\": \"2026-10-01\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(tenantContactUseCase, tenantUserDepartmentUseCase);
    }

    @Test
    void theTenantAdministratorPassesEveryRule() throws Exception {
        when(tenantUserDepartmentUseCase.findAllByTenantUserId(anyLong(), anyInt(), anyInt())).thenReturn(emptyPage());

        mockMvc.perform(delete("/usuarios/1")).andExpect(status().isOk());
        mockMvc.perform(delete("/corehr/shifts/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/corehr/schedules/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/corehr/schedules/1/tolerancias/2")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/corehr/user-schedules/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/corehr/non-working-days/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/corehr/tenant-contacts/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/corehr/devices/1")).andExpect(status().isNoContent());
        mockMvc.perform(get("/corehr/usuarios/1/departamentos")).andExpect(status().isOk());
    }
}
