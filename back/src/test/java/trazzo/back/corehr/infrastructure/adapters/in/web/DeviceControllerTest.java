package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.command.CreateDeviceCommand;
import trazzo.back.corehr.application.dto.command.PatchDeviceCommand;
import trazzo.back.corehr.application.dto.result.DeviceResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.port.in.DeviceUseCase;
import trazzo.back.corehr.domain.exception.InactiveDeviceException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeviceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DeviceUseCase deviceUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private static DeviceResult aResult() {
        return new DeviceResult(1L, "D-001", "Device1", 10L, "Branch1",
                "192.168.1.1", 8080, "Office", true, NOW);
    }

    @Test
    void list_shouldReturn200WithDeviceListResponse() throws Exception {
        var paginated = new PaginatedResult<DeviceResult>(List.of(aResult()), 0, 20, 1, 1);
        when(deviceUseCase.findAll(any(), any(), anyInt(), anyInt())).thenReturn(paginated);

        mockMvc.perform(get("/corehr/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("D-001"))
                .andExpect(jsonPath("$.content[0].state").value(true))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(deviceUseCase.create(any(CreateDeviceCommand.class))).thenReturn(aResult());

        mockMvc.perform(post("/corehr/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "D-001", "name": "Device1", "branch_id": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("D-001"));
    }

    @Test
    void create_shouldReturn400WhenCodeIsBlank() throws Exception {
        mockMvc.perform(post("/corehr/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "", "branch_id": 10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenBranchIdIsMissing() throws Exception {
        mockMvc.perform(post("/corehr/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "D-001"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn422WhenUseCaseThrowsInactiveDeviceException() throws Exception {
        when(deviceUseCase.create(any(CreateDeviceCommand.class)))
                .thenThrow(new InactiveDeviceException("Device is inactive"));

        mockMvc.perform(post("/corehr/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "D-002", "branch_id": 10}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Device is inactive"));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(deviceUseCase.findById(1L)).thenReturn(Optional.of(aResult()));

        mockMvc.perform(get("/corehr/devices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(deviceUseCase.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/corehr/devices/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_shouldReturn200() throws Exception {
        when(deviceUseCase.patch(anyLong(), any(PatchDeviceCommand.class))).thenReturn(aResult());

        mockMvc.perform(patch("/corehr/devices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Updated Device"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Device1"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(deviceUseCase).deleteById(1L);

        mockMvc.perform(delete("/corehr/devices/1"))
                .andExpect(status().isNoContent());
    }
}
