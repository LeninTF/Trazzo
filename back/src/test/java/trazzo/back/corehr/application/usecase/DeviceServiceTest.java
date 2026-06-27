package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.command.CreateDeviceCommand;
import trazzo.back.corehr.application.dto.command.PatchDeviceCommand;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.domain.model.attendance.Device;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class DeviceServiceTest {

    private DeviceRepositoryPort deviceRepository;
    private DeviceService service;

    @BeforeEach
    void setUp() {
        deviceRepository = mock(DeviceRepositoryPort.class);
        service = new DeviceService(deviceRepository);
    }

    @Test
    void createWithValidData() {
        when(deviceRepository.existsByCode("SN-001")).thenReturn(false);
        when(deviceRepository.save(any())).thenAnswer(invocation -> invocation.<Device>getArgument(0));

        var command = new CreateDeviceCommand("SN-001", "Device 1", 1L, "192.168.1.1", 8080, "Oficina");
        var result = service.create(command);

        assertEquals("SN-001", result.code());
        assertEquals("Device 1", result.name());
        assertTrue(result.state());
        verify(deviceRepository).save(any());
    }

    @Test
    void createWithDuplicateCodeThrowsException() {
        when(deviceRepository.existsByCode("SN-001")).thenReturn(true);

        var command = new CreateDeviceCommand("SN-001", "Device 1", 1L, "192.168.1.1", 8080, "Oficina");

        assertThrows(IllegalArgumentException.class, () -> service.create(command));
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void findByIdReturnsDevice() {
        var now = LocalDateTime.now();
        var device = Device.restore(1L, "SN-001", "Device 1", "192.168.1.1", 8080, "Oficina", 1L, true, now);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        var result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("SN-001", result.get().code());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(service.findById(99L).isEmpty());
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var device = Device.restore(1L, "SN-001", "Device 1", "192.168.1.1", 8080, "Oficina", 1L, true, now);
        when(deviceRepository.findAll(1L, true, 0, 10)).thenReturn(List.of(device));
        when(deviceRepository.count(1L, true)).thenReturn(1L);

        var result = service.findAll(1L, true, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
    }

    @Test
    void patchUpdatesLocation() {
        var now = LocalDateTime.now();
        var device = Device.restore(1L, "SN-001", "Device 1", "192.168.1.1", 8080, "Oficina", 1L, true, now);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any())).thenAnswer(invocation -> invocation.<Device>getArgument(0));

        var command = new PatchDeviceCommand(null, null, "10.0.0.1", 9090, "Nueva oficina", null);
        var result = service.patch(1L, command);

        assertEquals("10.0.0.1", result.ip());
        assertEquals(9090, result.puerto());
        assertEquals("Nueva oficina", result.ubicacion());
    }

    @Test
    void patchUpdatesState() {
        var now = LocalDateTime.now();
        var device = Device.restore(1L, "SN-001", "Device 1", "192.168.1.1", 8080, "Oficina", 1L, true, now);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any())).thenAnswer(invocation -> invocation.<Device>getArgument(0));

        var command = new PatchDeviceCommand(null, null, null, null, null, false);
        var result = service.patch(1L, command);

        assertFalse(result.state());
    }

    @Test
    void patchWithNotFoundIdThrowsException() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        var command = new PatchDeviceCommand("Nuevo", null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.patch(99L, command));
    }

    @Test
    void deleteByIdSuccess() {
        var now = LocalDateTime.now();
        var device = Device.restore(1L, "SN-001", "Device 1", "192.168.1.1", 8080, "Oficina", 1L, true, now);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        service.deleteById(1L);

        verify(deviceRepository).deleteById(1L);
    }

    @Test
    void deleteByIdThrowsExceptionWhenNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99L));
        verify(deviceRepository, never()).deleteById(any());
    }
}
