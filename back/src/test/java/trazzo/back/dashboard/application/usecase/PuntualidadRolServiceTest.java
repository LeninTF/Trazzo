package trazzo.back.dashboard.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.dashboard.application.port.out.DashboardAttendancePort;

import java.time.LocalDate;
import java.util.List;

class PuntualidadRolServiceTest {

    private DashboardAttendancePort attendancePort;
    private PuntualidadRolService service;

    @BeforeEach
    void setUp() {
        attendancePort = mock(DashboardAttendancePort.class);
        service = new PuntualidadRolService(attendancePort);
    }

    @Test
    void getPuntualidadPorRolReturnsPercentages() {
        LocalDate desde = LocalDate.of(2026, 7, 1);
        LocalDate hasta = LocalDate.of(2026, 7, 31);

        var projections = List.of(
                new DashboardAttendancePort.RolePunctualityProjection("Director", 100, 95),
                new DashboardAttendancePort.RolePunctualityProjection("Docentes", 50, 36));
        when(attendancePort.getPunctualityByRole(desde, hasta)).thenReturn(projections);

        var result = service.getPuntualidadPorRol(desde, hasta, "mes");

        assertEquals("mes", result.periodo());
        assertEquals(2, result.roles().size());
        assertEquals("Director", result.roles().get(0).nombre());
        assertEquals(95.0, result.roles().get(0).porcentaje(), 0.1);
        assertEquals("Docentes", result.roles().get(1).nombre());
        assertEquals(72.0, result.roles().get(1).porcentaje(), 0.1);
    }

    @Test
    void getPuntualidadPorRolWithNoDataReturnsZeros() {
        LocalDate desde = LocalDate.of(2026, 7, 1);
        LocalDate hasta = LocalDate.of(2026, 7, 31);

        when(attendancePort.getPunctualityByRole(desde, hasta)).thenReturn(List.of());

        var result = service.getPuntualidadPorRol(desde, hasta, "mes");

        assertTrue(result.roles().isEmpty());
    }
}
