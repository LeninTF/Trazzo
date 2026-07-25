package trazzo.back.dashboard.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.dashboard.application.port.out.DashboardAttendancePort;
import trazzo.back.dashboard.application.port.out.DashboardIncidentPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

class DashboardSummaryServiceTest {

    private DashboardAttendancePort attendancePort;
    private DashboardIncidentPort incidentPort;
    private DashboardSummaryService service;

    @BeforeEach
    void setUp() {
        attendancePort = mock(DashboardAttendancePort.class);
        incidentPort = mock(DashboardIncidentPort.class);
        service = new DashboardSummaryService(attendancePort, incidentPort);
    }

    @Test
    void getSummaryReturnsPopulatedData() {
        LocalDate desde = LocalDate.of(2026, 7, 1);
        LocalDate hasta = LocalDate.of(2026, 7, 31);

        when(attendancePort.countActiveUsers()).thenReturn(20L);
        when(attendancePort.countByStateAndDateRange("FALTA", desde, hasta)).thenReturn(5L);
        when(incidentPort.countByDateRange(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())).thenReturn(3L);
        when(attendancePort.countPunctualByYear(2026)).thenReturn(400L);
        when(attendancePort.countTotalByYear(2026)).thenReturn(500L);

        var result = service.getSummary(desde, hasta);

        assertEquals(20L, result.usuariosActivos());
        assertEquals(5L, result.metricas().totalInasistencias());
        assertEquals(3L, result.metricas().totalIncidencias());
        assertEquals(80.0, result.indicePuntualidadAnual(), 0.1);
    }

    @Test
    void getSummaryWithNoAttendanceReturnsZeroPuntuality() {
        LocalDate desde = LocalDate.of(2026, 7, 1);
        LocalDate hasta = LocalDate.of(2026, 7, 31);

        when(attendancePort.countActiveUsers()).thenReturn(0L);
        when(attendancePort.countByStateAndDateRange("FALTA", desde, hasta)).thenReturn(0L);
        when(incidentPort.countByDateRange(any(), any())).thenReturn(0L);
        when(attendancePort.countPunctualByYear(2026)).thenReturn(0L);
        when(attendancePort.countTotalByYear(2026)).thenReturn(0L);

        var result = service.getSummary(desde, hasta);

        assertEquals(0L, result.usuariosActivos());
        assertEquals(0.0, result.indicePuntualidadAnual());
    }
}
