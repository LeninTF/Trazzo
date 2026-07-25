package trazzo.back.dashboard.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.dashboard.application.port.out.DashboardAttendancePort;

import java.util.List;

class DashboardAlertsServiceTest {

    private DashboardAttendancePort attendancePort;
    private DashboardAlertsService service;

    @BeforeEach
    void setUp() {
        attendancePort = mock(DashboardAttendancePort.class);
        service = new DashboardAlertsService(attendancePort);
    }

    @Test
    void getAlertasReturnsTardanzasWhenPresent() {
        when(attendancePort.countByStateAndDateRange(eq("TARDANZA"), any(), any())).thenReturn(5L);
        when(attendancePort.getUserIdsWithoutAttendance(any())).thenReturn(List.of());

        var result = service.getAlertas();

        assertEquals(1, result.alertas().size());
        assertEquals("Tardanzas hoy", result.alertas().get(0).titulo());
        assertEquals("danger", result.alertas().get(0).tipo());
    }

    @Test
    void getAlertasReturnsAusentesWhenPresent() {
        when(attendancePort.countByStateAndDateRange(eq("TARDANZA"), any(), any())).thenReturn(0L);
        when(attendancePort.getUserIdsWithoutAttendance(any())).thenReturn(List.of(1L, 2L));

        var result = service.getAlertas();

        assertEquals(1, result.alertas().size());
        assertEquals("Personal sin registro", result.alertas().get(0).titulo());
        assertEquals("warning", result.alertas().get(0).tipo());
    }

    @Test
    void getAlertasReturnsEmptyWhenNoIssues() {
        when(attendancePort.countByStateAndDateRange(eq("TARDANZA"), any(), any())).thenReturn(0L);
        when(attendancePort.getUserIdsWithoutAttendance(any())).thenReturn(List.of());

        var result = service.getAlertas();

        assertTrue(result.alertas().isEmpty());
    }
}
