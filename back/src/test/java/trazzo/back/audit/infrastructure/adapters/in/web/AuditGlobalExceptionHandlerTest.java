package trazzo.back.audit.infrastructure.adapters.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.exception.AuditValidationException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditGlobalExceptionHandlerTest {

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new AuditGlobalExceptionHandler())
                .build();
    }

    @RestController
    static class StubController {
        @GetMapping("/throw/{type}")
        void throwException(@PathVariable String type) {
            switch (type) {
                case "not-found" -> throw new AuditNotFoundException("Audit not found");
                case "illegal-arg" -> throw new IllegalArgumentException("Bad argument");
                case "illegal-state" -> throw new IllegalStateException("Bad state");
                case "validation" -> throw new AuditValidationException("Validation failed");
                default -> throw new RuntimeException("Unexpected error");
            }
        }
    }

    @Test
    void auditNotFound_returns404() throws Exception {
        mockMvc.perform(get("/throw/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Audit not found"));
    }

    @Test
    void illegalArgument_returns400BadRequest() throws Exception {
        mockMvc.perform(get("/throw/illegal-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Bad argument"));
    }

    @Test
    void illegalState_returns400BadRequest() throws Exception {
        mockMvc.perform(get("/throw/illegal-state"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Bad state"));
    }

    @Test
    void auditValidation_returns400ValidationError() throws Exception {
        mockMvc.perform(get("/throw/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void generic_returns500InternalServerError() throws Exception {
        mockMvc.perform(get("/throw/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected error occurred"));
    }

    @Test
    void timestampIsPresent() throws Exception {
        mockMvc.perform(get("/throw/illegal-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void detailsIsNullForNonValidationErrors() throws Exception {
        mockMvc.perform(get("/throw/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details").doesNotExist());
    }
}
