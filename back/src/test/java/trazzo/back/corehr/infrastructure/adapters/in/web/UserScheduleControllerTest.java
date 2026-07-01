package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.command.CreateUserScheduleCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ShiftResult.ScheduleSummary;
import trazzo.back.corehr.application.dto.result.UserScheduleResult;
import trazzo.back.corehr.application.port.in.UserScheduleUseCase;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserScheduleControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserScheduleUseCase userScheduleUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalTime EIGHT_AM = LocalTime.of(8, 0);
    private static final LocalTime FIVE_PM = LocalTime.of(17, 0);

    private static UserScheduleResult aResult() {
        var summary = new ScheduleSummary(1L, "Morning");
        return new UserScheduleResult(1L, 10L, 1L, summary, "desc", EIGHT_AM, FIVE_PM, NOW, NOW);
    }

    @Test
    void list_shouldReturn200() throws Exception {
        var paginated = new PaginatedResult<UserScheduleResult>(List.of(aResult()), 0, 20, 1, 1);
        when(userScheduleUseCase.findAll(any(), any(), anyInt(), anyInt())).thenReturn(paginated);

        mockMvc.perform(get("/corehr/user-schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entry_time").value("08:00:00"))
                .andExpect(jsonPath("$.content[0].schedule.name").value("Morning"))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void list_shouldFilterByTenantUserAndSchedule() throws Exception {
        var paginated = new PaginatedResult<UserScheduleResult>(List.of(aResult()), 0, 10, 1, 1);
        when(userScheduleUseCase.findAll(eq(10L), eq(1L), eq(0), eq(10))).thenReturn(paginated);

        mockMvc.perform(get("/corehr/user-schedules")
                        .param("tenant_user_id", "10")
                        .param("schedule_id", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(userScheduleUseCase.create(any(CreateUserScheduleCommand.class))).thenReturn(aResult());

        mockMvc.perform(post("/corehr/user-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenant_user_id": 10, "schedule_id": 1, "entry_time": "08:00", "departure_time": "17:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entry_time").value("08:00:00"));
    }

    @Test
    void create_shouldReturn400WhenTenantUserIdIsNull() throws Exception {
        mockMvc.perform(post("/corehr/user-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schedule_id": 1, "entry_time": "08:00", "departure_time": "17:00"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenScheduleIdIsNull() throws Exception {
        mockMvc.perform(post("/corehr/user-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenant_user_id": 10, "entry_time": "08:00", "departure_time": "17:00"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenEntryTimeIsNull() throws Exception {
        mockMvc.perform(post("/corehr/user-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenant_user_id": 10, "schedule_id": 1, "departure_time": "17:00"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(userScheduleUseCase).deleteById(1L);

        mockMvc.perform(delete("/corehr/user-schedules/1"))
                .andExpect(status().isNoContent());
    }
}
