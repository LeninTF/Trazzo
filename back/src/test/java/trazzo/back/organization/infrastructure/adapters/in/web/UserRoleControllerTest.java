package trazzo.back.organization.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import trazzo.back.organization.application.dto.result.UserRoleAssignmentResult;
import trazzo.back.organization.application.port.in.UserRoleUseCase;
import trazzo.back.organization.domain.exception.OrgNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserRoleControllerTest {

    @Mock UserRoleUseCase userRoleUseCase;
    @InjectMocks UserRoleController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new OrgGlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    private UserRoleAssignmentResult stub() {
        return new UserRoleAssignmentResult(1L, 10L, "r-1", null, LocalDateTime.now());
    }

    @Test
    void list_returnsOk() throws Exception {
        when(userRoleUseCase.findByTenantUserId(10L)).thenReturn(List.of(stub()));

        mockMvc.perform(get("/org/users/10/roles"))
                .andExpect(status().isOk());
    }

    @Test
    void assign_validRequest_returnsCreated() throws Exception {
        when(userRoleUseCase.assign(any(), any())).thenReturn(stub());

        mockMvc.perform(post("/org/users/10/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":\"r-1\",\"departmentId\":null}"))
                .andExpect(status().isCreated());
    }

    @Test
    void remove_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/org/users/10/roles/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void remove_notFound_returnsNotFound() throws Exception {
        doThrow(new OrgNotFoundException("not found")).when(userRoleUseCase).remove(10L, 99L);

        mockMvc.perform(delete("/org/users/10/roles/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void assign_roleNotFound_returnsNotFound() throws Exception {
        when(userRoleUseCase.assign(any(), any())).thenThrow(new OrgNotFoundException("role not found"));

        mockMvc.perform(post("/org/users/10/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":\"x\",\"departmentId\":null}"))
                .andExpect(status().isNotFound());
    }
}
