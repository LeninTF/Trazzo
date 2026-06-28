package trazzo.back.reports.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import trazzo.back.reports.application.dto.command.CreateMonthlyClosureCommand;
import trazzo.back.reports.application.dto.command.GetMonthlyClosureCommand;
import trazzo.back.reports.application.dto.command.ListMonthlyClosuresCommand;
import trazzo.back.reports.application.dto.result.MonthlyClosureResult;
import trazzo.back.reports.application.ports.in.CreateMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.in.GetMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.in.ListMonthlyClosureUseCase;
import trazzo.back.reports.infrastructure.adapters.in.web.dto.CreateMonthlyClosureRequest;
import trazzo.back.shared.security.AuthenticatedUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@WebMvcTest(controllers = MonthlyClosureController.class)
class MonthlyClosureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateMonthlyClosureUseCase createUseCase;

    @MockitoBean
    private GetMonthlyClosureUseCase getUseCase;

    @MockitoBean
    private ListMonthlyClosureUseCase listUseCase;

    private RequestPostProcessor authPostProcessor;

    @BeforeEach
    void setUp() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(UUID.randomUUID(), "admin@trazzo.com", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), true),
                null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        authPostProcessor = authentication(auth);
    }

    @Test
    void shouldCreateClosure() throws Exception {
        CreateMonthlyClosureRequest request = new CreateMonthlyClosureRequest(6, 2025);
        MonthlyClosureResult result = new MonthlyClosureResult(UUID.randomUUID(), 6, 2025, 5, "excel", "pdf", LocalDateTime.now());

        when(createUseCase.execute(any(CreateMonthlyClosureCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/reports/monthly-closures")
                        .with(authPostProcessor)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(result.id().toString()))
                .andExpect(jsonPath("$.month").value(6))
                .andExpect(jsonPath("$.year").value(2025));
    }

    @Test
    void shouldReturnClosureById() throws Exception {
        UUID id = UUID.randomUUID();
        MonthlyClosureResult result = new MonthlyClosureResult(id, 6, 2025, 10, "excel", "pdf", LocalDateTime.now());

        when(getUseCase.execute(any(GetMonthlyClosureCommand.class))).thenReturn(result);

        mockMvc.perform(get("/api/v1/reports/monthly-closures/{id}", id)
                        .with(authPostProcessor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.month").value(6));
    }

    @Test
    void shouldListClosures() throws Exception {
        MonthlyClosureResult r1 = new MonthlyClosureResult(UUID.randomUUID(), 6, 2025, 10, "e1", "p1", LocalDateTime.now());
        MonthlyClosureResult r2 = new MonthlyClosureResult(UUID.randomUUID(), 7, 2025, 5, "e2", "p2", LocalDateTime.now());

        when(listUseCase.execute(any(ListMonthlyClosuresCommand.class))).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/reports/monthly-closures")
                        .with(authPostProcessor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldListClosuresWithFilters() throws Exception {
        MonthlyClosureResult result = new MonthlyClosureResult(UUID.randomUUID(), 6, 2025, 10, "e", "p", LocalDateTime.now());

        when(listUseCase.execute(any(ListMonthlyClosuresCommand.class))).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/reports/monthly-closures")
                        .with(authPostProcessor)
                        .param("year", "2025")
                        .param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
