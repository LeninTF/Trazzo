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
import trazzo.back.organization.application.dto.result.RolePermissionResult;
import trazzo.back.organization.application.dto.result.RoleResult;
import trazzo.back.organization.application.dto.result.UserRoleAssignmentResult;
import trazzo.back.organization.application.port.in.RolePermissionsUseCase;
import trazzo.back.organization.application.port.in.RoleUseCase;
import trazzo.back.organization.application.port.in.UserRoleUseCase;
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
class RoleControllerTest {

    @Mock RoleUseCase roleUseCase;
    @Mock RolePermissionsUseCase rolePermissionsUseCase;
    @Mock UserRoleUseCase userRoleUseCase;
    @InjectMocks RoleController controller;

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

    private RoleResult stubRole() {
        return new RoleResult("r-1", "Admin", "desc", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void list_returnsOk() throws Exception {
        when(roleUseCase.findAll(any(), anyInt(), anyInt(), any()))
                .thenReturn(new PaginatedResult<>(List.of(stubRole()), 0, 20, 1L, 1));

        mockMvc.perform(get("/org/roles"))
                .andExpect(status().isOk());
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(roleUseCase.create(any())).thenReturn(stubRole());

        mockMvc.perform(post("/org/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin\",\"description\":\"desc\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getById_found_returnsOk() throws Exception {
        when(roleUseCase.findById("r-1")).thenReturn(Optional.of(stubRole()));

        mockMvc.perform(get("/org/roles/r-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returnsNotFound() throws Exception {
        when(roleUseCase.findById("x")).thenReturn(Optional.empty());

        mockMvc.perform(get("/org/roles/x"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returnsOk() throws Exception {
        when(roleUseCase.update(any(), any())).thenReturn(stubRole());

        mockMvc.perform(put("/org/roles/r-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"description\":\"desc\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/org/roles/r-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listPermissions_returnsOk() throws Exception {
        when(rolePermissionsUseCase.findByRoleId("r-1"))
                .thenReturn(List.of(new RolePermissionResult("r-1", "p-1", LocalDateTime.now())));

        mockMvc.perform(get("/org/roles/r-1/permissions"))
                .andExpect(status().isOk());
    }

    @Test
    void assignPermission_returnsCreated() throws Exception {
        when(rolePermissionsUseCase.assign(any(), any()))
                .thenReturn(new RolePermissionResult("r-1", "p-1", LocalDateTime.now()));

        mockMvc.perform(post("/org/roles/r-1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionId\":\"p-1\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void removePermission_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/org/roles/r-1/permissions/p-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listUsers_returnsOk() throws Exception {
        when(userRoleUseCase.findByRoleId("r-1"))
                .thenReturn(List.of(new UserRoleAssignmentResult(1L, 10L, "r-1", null, LocalDateTime.now())));

        mockMvc.perform(get("/org/roles/r-1/users"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_notFound_returnsNotFound() throws Exception {
        doThrow(new OrgNotFoundException("not found")).when(roleUseCase).delete("x");

        mockMvc.perform(delete("/org/roles/x"))
                .andExpect(status().isNotFound());
    }
}
