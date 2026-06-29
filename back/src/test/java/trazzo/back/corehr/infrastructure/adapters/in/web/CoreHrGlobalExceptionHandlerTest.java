package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.corehr.domain.exception.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CoreHrGlobalExceptionHandlerTest {

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new CoreHrGlobalExceptionHandler())
                .build();
    }

    @RestController
    static class StubController {
        @GetMapping("/throw/{type}")
        void throwException(@PathVariable String type) {
            switch (type) {
                case "illegal-arg" -> throw new IllegalArgumentException("Bad argument");
                case "illegal-state" -> throw new IllegalStateException("Bad state");
                case "validation" -> throw new CoreHrValidationException("Validation failed");
                case "invalid-attendance" -> throw new InvalidAttendanceException("Attendance error");
                case "inactive-device" -> throw new InactiveDeviceException("Device inactive");
                case "invalid-schedule" -> throw new InvalidScheduleException("Schedule error");
                case "invalid-shift" -> throw new InvalidShiftException("Shift error");
                case "invalid-tolerancia" -> throw new InvalidToleranciaException("Tolerancia error");
                case "invalid-nwd" -> throw new InvalidNonWorkingDaysException("NWD error");
                case "invalid-tenant-user" -> throw new InvalidTenantUserException("Tenant user error");
                default -> throw new RuntimeException("Unexpected");
            }
        }
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
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Bad state"));
    }

    @Test
    void coreHrValidation_returns400ValidationError() throws Exception {
        mockMvc.perform(get("/throw/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void invalidAttendance_returns422() throws Exception {
        mockMvc.perform(get("/throw/invalid-attendance"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Attendance error"));
    }

    @Test
    void inactiveDevice_returns422() throws Exception {
        mockMvc.perform(get("/throw/inactive-device"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Device inactive"));
    }

    @Test
    void invalidSchedule_returns422() throws Exception {
        mockMvc.perform(get("/throw/invalid-schedule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Schedule error"));
    }

    @Test
    void invalidShift_returns422() throws Exception {
        mockMvc.perform(get("/throw/invalid-shift"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Shift error"));
    }

    @Test
    void invalidTolerancia_returns422() throws Exception {
        mockMvc.perform(get("/throw/invalid-tolerancia"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Tolerancia error"));
    }

    @Test
    void invalidNonWorkingDays_returns422() throws Exception {
        mockMvc.perform(get("/throw/invalid-nwd"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("NWD error"));
    }

    @Test
    void invalidTenantUser_returns422() throws Exception {
        mockMvc.perform(get("/throw/invalid-tenant-user"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath(".error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Tenant user error"));
    }

    @Test
    void timestampIsPresentInResponse() throws Exception {
        mockMvc.perform(get("/throw/illegal-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void detailsIsNullForNonValidationErrors() throws Exception {
        mockMvc.perform(get("/throw/invalid-attendance"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details").doesNotExist());
    }
}
