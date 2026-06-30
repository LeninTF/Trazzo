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
import trazzo.back.organization.application.dto.result.DepartmentResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.port.in.DepartmentUseCase;
import trazzo.back.organization.domain.exception.OrgNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DepartmentControllerTest {

    @Mock DepartmentUseCase departmentUseCase;
    @InjectMocks DepartmentController controller;

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

    private DepartmentResult stub() {
        return new DepartmentResult(1L, 1L, "HR", "desc", true,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void list_returnsOk() throws Exception {
        when(departmentUseCase.findAll(any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PaginatedResult<>(List.of(stub()), 0, 20, 1L, 1));

        mockMvc.perform(get("/org/departments"))
                .andExpect(status().isOk());
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(departmentUseCase.create(any())).thenReturn(stub());

        mockMvc.perform(post("/org/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"areaId\":1,\"name\":\"HR\",\"description\":\"desc\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getById_found_returnsOk() throws Exception {
        when(departmentUseCase.findById(1L)).thenReturn(Optional.of(stub()));

        mockMvc.perform(get("/org/departments/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returnsNotFound() throws Exception {
        when(departmentUseCase.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/org/departments/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returnsOk() throws Exception {
        when(departmentUseCase.update(any(), any())).thenReturn(stub());

        mockMvc.perform(put("/org/departments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"description\":\"desc\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/org/departments/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void create_areaNotFound_returnsNotFound() throws Exception {
        when(departmentUseCase.create(any())).thenThrow(new OrgNotFoundException("area not found"));

        mockMvc.perform(post("/org/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"areaId\":99,\"name\":\"HR\",\"description\":\"desc\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_notFound_returnsNotFound() throws Exception {
        doThrow(new OrgNotFoundException("not found")).when(departmentUseCase).delete(99L);

        mockMvc.perform(delete("/org/departments/99"))
                .andExpect(status().isNotFound());
    }
}
