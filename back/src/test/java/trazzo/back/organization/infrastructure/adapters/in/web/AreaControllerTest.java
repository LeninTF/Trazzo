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
import trazzo.back.organization.application.dto.result.AreaResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.port.in.AreaUseCase;
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
class AreaControllerTest {

    @Mock AreaUseCase areaUseCase;
    @InjectMocks AreaController controller;

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

    private AreaResult stub() {
        return new AreaResult(1L, 1L, "Sales", "desc", true,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void list_returnsOk() throws Exception {
        when(areaUseCase.findAll(any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PaginatedResult<>(List.of(stub()), 0, 20, 1L, 1));

        mockMvc.perform(get("/org/areas"))
                .andExpect(status().isOk());
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(areaUseCase.create(any())).thenReturn(stub());

        mockMvc.perform(post("/org/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":1,\"name\":\"Sales\",\"description\":\"desc\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getById_found_returnsOk() throws Exception {
        when(areaUseCase.findById(1L)).thenReturn(Optional.of(stub()));

        mockMvc.perform(get("/org/areas/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returnsNotFound() throws Exception {
        when(areaUseCase.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/org/areas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returnsOk() throws Exception {
        when(areaUseCase.update(any(), any())).thenReturn(stub());

        mockMvc.perform(put("/org/areas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"description\":\"desc\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/org/areas/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returnsNotFound() throws Exception {
        doThrow(new OrgNotFoundException("not found")).when(areaUseCase).delete(99L);

        mockMvc.perform(delete("/org/areas/99"))
                .andExpect(status().isNotFound());
    }
}
