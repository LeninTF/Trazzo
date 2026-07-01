package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.command.CreateNonWorkingDayCommand;
import trazzo.back.corehr.application.dto.command.PatchNonWorkingDayCommand;
import trazzo.back.corehr.application.dto.result.NonWorkingDayResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.port.in.NonWorkingDayUseCase;
import trazzo.back.corehr.domain.exception.InvalidNonWorkingDaysException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NonWorkingDayController.class)
@AutoConfigureMockMvc(addFilters = false)
class NonWorkingDayControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NonWorkingDayUseCase nonWorkingDayUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDate CHRISTMAS = LocalDate.of(2025, 12, 25);

    private static NonWorkingDayResult aResult() {
        return new NonWorkingDayResult(1L, CHRISTMAS, "Navidad", true, NOW);
    }

    @Test
    void list_shouldReturn200() throws Exception {
        var paginated = new PaginatedResult<NonWorkingDayResult>(List.of(aResult()), 0, 20, 1, 1);
        when(nonWorkingDayUseCase.findAll(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(paginated);

        mockMvc.perform(get("/corehr/non-working-days"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Navidad"))
                .andExpect(jsonPath("$.content[0].is_recurring").value(true))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void list_shouldFilterByDateRange() throws Exception {
        var paginated = new PaginatedResult<NonWorkingDayResult>(List.of(aResult()), 0, 10, 1, 1);
        when(nonWorkingDayUseCase.findAll(eq(CHRISTMAS), eq(CHRISTMAS), any(), anyInt(), anyInt()))
                .thenReturn(paginated);

        mockMvc.perform(get("/corehr/non-working-days")
                        .param("date_from", "2025-12-25")
                        .param("date_to", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].date").value("2025-12-25"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(nonWorkingDayUseCase.create(any(CreateNonWorkingDayCommand.class)))
                .thenReturn(aResult());

        mockMvc.perform(post("/corehr/non-working-days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date": "2025-12-25", "description": "Navidad", "is_recurring": true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Navidad"));
    }

    @Test
    void create_shouldReturn400WhenDateIsNull() throws Exception {
        mockMvc.perform(post("/corehr/non-working-days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Navidad"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn422WhenUseCaseThrowsInvalidNonWorkingDaysException() throws Exception {
        when(nonWorkingDayUseCase.create(any(CreateNonWorkingDayCommand.class)))
                .thenThrow(new InvalidNonWorkingDaysException("Invalid date"));

        mockMvc.perform(post("/corehr/non-working-days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date": "2025-12-25"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Invalid date"));
    }

    @Test
    void patch_shouldReturn200() throws Exception {
        when(nonWorkingDayUseCase.patch(anyLong(), any(PatchNonWorkingDayCommand.class)))
                .thenReturn(aResult());

        mockMvc.perform(patch("/corehr/non-working-days/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Navidad"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(nonWorkingDayUseCase).deleteById(1L);

        mockMvc.perform(delete("/corehr/non-working-days/1"))
                .andExpect(status().isNoContent());
    }
}
