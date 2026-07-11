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
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.dto.result.PermissionResult;
import trazzo.back.organization.application.port.in.PermissionUseCase;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
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
class PermissionControllerTest {

    @Mock PermissionUseCase permissionUseCase;
    @InjectMocks PermissionController controller;

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

    private PermissionResult stub() {
        return new PermissionResult("p-1", "READ", "read access", "CODE", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void list_returnsOk() throws Exception {
        when(permissionUseCase.findAll(any(), anyInt(), anyInt(), any()))
                .thenReturn(new PaginatedResult<>(List.of(stub()), 0, 20, 1L, 1));

        mockMvc.perform(get("/org/permissions"))
                .andExpect(status().isOk());
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(permissionUseCase.create(any())).thenReturn(stub());

        mockMvc.perform(post("/org/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"READ\",\"description\":\"desc\",\"masterFeaturesCode\":\"CODE\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getById_found_returnsOk() throws Exception {
        when(permissionUseCase.findById("p-1")).thenReturn(Optional.of(stub()));

        mockMvc.perform(get("/org/permissions/p-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returnsNotFound() throws Exception {
        when(permissionUseCase.findById("x")).thenReturn(Optional.empty());

        mockMvc.perform(get("/org/permissions/x"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returnsOk() throws Exception {
        when(permissionUseCase.update(any(), any())).thenReturn(stub());

        mockMvc.perform(put("/org/permissions/p-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"WRITE\",\"description\":\"desc\",\"masterFeaturesCode\":\"CODE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/org/permissions/p-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void create_duplicate_returnsConflict() throws Exception {
        when(permissionUseCase.create(any())).thenThrow(new DuplicateOrgNameException("READ already exists"));

        mockMvc.perform(post("/org/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"READ\",\"description\":\"desc\",\"masterFeaturesCode\":\"CODE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_notFound_returnsNotFound() throws Exception {
        doThrow(new OrgNotFoundException("not found")).when(permissionUseCase).delete("x");

        mockMvc.perform(delete("/org/permissions/x"))
                .andExpect(status().isNotFound());
    }
}
