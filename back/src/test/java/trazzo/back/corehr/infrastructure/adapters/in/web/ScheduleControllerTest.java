package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.command.CreateScheduleCommand;
import trazzo.back.corehr.application.dto.command.PatchScheduleCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ScheduleResult;
import trazzo.back.corehr.application.dto.result.ScheduleResult.ShiftSummary;
import trazzo.back.corehr.application.port.in.ScheduleUseCase;
import trazzo.back.corehr.domain.exception.InvalidScheduleException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
class ScheduleControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ScheduleUseCase scheduleUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalTime EIGHT_AM = LocalTime.of(8, 0);
    private static final LocalTime FIVE_PM = LocalTime.of(17, 0);

    private static ScheduleResult aResult() {
        var shift = new ShiftSummary(1L, "Morning");
        return new ScheduleResult(1L, 1L, shift, "Schedule1", "desc",
                EIGHT_AM, FIVE_PM, List.of(), NOW, NOW);
    }

    @Test
    void list_shouldReturn200() throws Exception {
        var paginated = new PaginatedResult<ScheduleResult>(List.of(aResult()), 0, 20, 1, 1);
        when(scheduleUseCase.findAll(any(), anyInt(), anyInt(), any())).thenReturn(paginated);

        mockMvc.perform(get("/corehr/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Schedule1"))
                .andExpect(jsonPath("$.content[0].entry_time").value("08:00:00"))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void list_shouldFilterByShiftId() throws Exception {
        var paginated = new PaginatedResult<ScheduleResult>(List.of(aResult()), 0, 10, 1, 1);
        when(scheduleUseCase.findAll(eq(1L), anyInt(), anyInt(), any())).thenReturn(paginated);

        mockMvc.perform(get("/corehr/schedules")
                        .param("shift_id", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(scheduleUseCase.create(any(CreateScheduleCommand.class))).thenReturn(aResult());

        mockMvc.perform(post("/corehr/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shift_id": 1, "name": "Schedule1", "entry_time": "08:00", "departure_time": "17:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Schedule1"));
    }

    @Test
    void create_shouldReturn400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/corehr/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shift_id": 1, "name": "", "entry_time": "08:00", "departure_time": "17:00"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenShiftIdIsNull() throws Exception {
        mockMvc.perform(post("/corehr/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "S1", "entry_time": "08:00", "departure_time": "17:00"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn422WhenUseCaseThrowsInvalidScheduleException() throws Exception {
        when(scheduleUseCase.create(any(CreateScheduleCommand.class)))
                .thenThrow(new InvalidScheduleException("Schedule conflict"));

        mockMvc.perform(post("/corehr/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shift_id": 1, "name": "S1", "entry_time": "08:00", "departure_time": "17:00"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Schedule conflict"));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(scheduleUseCase.findById(1L)).thenReturn(Optional.of(aResult()));

        mockMvc.perform(get("/corehr/schedules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(scheduleUseCase.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/corehr/schedules/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_shouldReturn200() throws Exception {
        when(scheduleUseCase.patch(anyLong(), any(PatchScheduleCommand.class))).thenReturn(aResult());

        mockMvc.perform(patch("/corehr/schedules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Schedule1"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(scheduleUseCase).deleteById(1L);

        mockMvc.perform(delete("/corehr/schedules/1"))
                .andExpect(status().isNoContent());
    }
}
