package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.SubscriptionResult;
import trazzo.back.saasglobal.application.port.in.SubscriptionUseCase;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SubscriptionUseCase subscriptionUseCase;

    private static SubscriptionResult result() {
        return new SubscriptionResult("sub-1", "tenant-1", "demo", 1, "Plan Demo",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ACTIVE",
                new BigDecimal("29.99"), LocalDateTime.now());
    }

    @Test
    @WithMockUser
    void listAll_returns200() throws Exception {
        when(subscriptionUseCase.listAll(0, 20)).thenReturn(PaginatedResult.of(List.of(result()), 0, 20, 1));

        mockMvc.perform(get("/saas/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("sub-1"))
                .andExpect(jsonPath("$.content[0].tenantName").value("demo"))
                .andExpect(jsonPath("$.content[0].planName").value("Plan Demo"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void listAll_returnsEmptyWhenNoSubscriptions() throws Exception {
        when(subscriptionUseCase.listAll(0, 20)).thenReturn(PaginatedResult.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/saas/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listAll_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/saas/subscriptions"))
                .andExpect(status().isUnauthorized());
    }
}
