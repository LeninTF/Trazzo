package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.command.PatchAttendanceCommand;
import trazzo.back.corehr.application.dto.result.AttendanceResult;
import trazzo.back.corehr.application.dto.result.AttendanceResult.TenantUserBasicInfo;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ShiftResult.ScheduleSummary;
import trazzo.back.corehr.application.port.in.AttendanceUseCase;
import trazzo.back.corehr.domain.exception.InvalidAttendanceException;
import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttendanceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AttendanceUseCase attendanceUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDate TODAY = LocalDate.now();

    private static AttendanceResult aResult() {
        var user = new TenantUserBasicInfo("u1", "Juan", "Perez");
        var schedule = new ScheduleSummary(1L, "Morning");
        return new AttendanceResult("att-1", 10L, user, 1L, schedule, 5L,
                "DVC-001", NOW, null, TODAY, 0, AttendanceState.PUNTUAL, NOW, NOW);
    }

    @Test
    void list_shouldReturn200WithAttendanceListResponse() throws Exception {
        var paginated = new PaginatedResult<AttendanceResult>(List.of(aResult()), 0, 20, 1, 1);
        when(attendanceUseCase.findAll(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(paginated);

        mockMvc.perform(get("/corehr/attendance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("att-1"))
                .andExpect(jsonPath("$.content[0].tenant_user_id").value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.scopeAplicado").isEmpty());
    }

    @Test
    void list_shouldPassQueryParams() throws Exception {
        var paginated = new PaginatedResult<AttendanceResult>(List.of(), 1, 5, 0, 0);
        when(attendanceUseCase.findAll(eq("personal"), eq(1L), any(), any(),
                any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(paginated);

        mockMvc.perform(get("/corehr/attendance")
                        .param("scope", "personal")
                        .param("branch_id", "1")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(attendanceUseCase.findById("att-1")).thenReturn(Optional.of(aResult()));

        mockMvc.perform(get("/corehr/attendance/att-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("att-1"))
                .andExpect(jsonPath("$.state").value("PUNTUAL"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(attendanceUseCase.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/corehr/attendance/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void correct_shouldReturn200() throws Exception {
        when(attendanceUseCase.correct(eq("att-1"), any(PatchAttendanceCommand.class)))
                .thenReturn(aResult());

        mockMvc.perform(patch("/corehr/attendance/att-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"check_in": "2025-06-01T08:00:00", "state": "PUNTUAL", "minutes_late": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("att-1"));
    }

    @Test
    void correct_shouldReturn400WhenMinutesLateIsNegative() throws Exception {
        mockMvc.perform(patch("/corehr/attendance/att-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"minutes_late": -5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void correct_shouldReturn422WhenUseCaseThrowsInvalidAttendanceException() throws Exception {
        when(attendanceUseCase.correct(anyString(), any(PatchAttendanceCommand.class)))
                .thenThrow(new InvalidAttendanceException("Attendance is invalid"));

        mockMvc.perform(patch("/corehr/attendance/att-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state": "TARDANZA", "minutes_late": 5}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Attendance is invalid"));
    }
}
