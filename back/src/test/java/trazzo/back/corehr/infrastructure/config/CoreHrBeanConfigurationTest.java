package trazzo.back.corehr.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.corehr.application.port.in.CoreHrAttendanceSummaryPort;
import trazzo.back.corehr.application.port.out.*;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollService;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollSessionStore;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class CoreHrBeanConfigurationTest {

    private final CoreHrBeanConfiguration config = new CoreHrBeanConfiguration();

    @Mock private ShiftRepositoryPort shiftRepo;
    @Mock private ScheduleRepositoryPort scheduleRepo;
    @Mock private ToleranciaRepositoryPort toleranciaRepo;
    @Mock private UserScheduleRepositoryPort userScheduleRepo;
    @Mock private DeviceRepositoryPort deviceRepo;
    @Mock private UserBiometriaRepositoryPort userBiometriaRepo;
    @Mock private AttendanceRepositoryPort attendanceRepo;
    @Mock private EventPublisherPort eventPublisher;
    @Mock private NonWorkingDaysRepositoryPort nonWorkingDaysRepo;
    @Mock private TenantContactRepositoryPort tenantContactRepo;
    @Mock private TenantUserPort tenantUserPort;
    @Mock private TenantUserDepartmentRepositoryPort deptRepo;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private EnrollSessionStore enrollSessionStore;
    @Mock private EnrollService enrollService;

    @Test
    void shouldCreateShiftUseCase() {
        assertNotNull(config.shiftUseCase(shiftRepo, scheduleRepo));
    }

    @Test
    void shouldCreateScheduleUseCase() {
        assertNotNull(config.scheduleUseCase(scheduleRepo, shiftRepo, toleranciaRepo, userScheduleRepo));
    }

    @Test
    void shouldCreateToleranciaUseCase() {
        assertNotNull(config.toleranciaUseCase(toleranciaRepo, scheduleRepo));
    }

    @Test
    void shouldCreateUserScheduleUseCase() {
        assertNotNull(config.userScheduleUseCase(userScheduleRepo, scheduleRepo, tenantUserPort));
    }

    @Test
    void shouldCreateDeviceUseCase() {
        assertNotNull(config.deviceUseCase(deviceRepo));
    }

    @Test
    void shouldCreateUserBiometriaUseCase() {
        assertNotNull(config.userBiometriaUseCase(userBiometriaRepo, enrollService));
    }

    @Test
    void shouldCreateAttendanceUseCase() {
        assertNotNull(config.attendanceUseCase(attendanceRepo, eventPublisher));
    }

    @Test
    void shouldCreateNonWorkingDayUseCase() {
        assertNotNull(config.nonWorkingDayUseCase(nonWorkingDaysRepo));
    }

    @Test
    void shouldCreateTenantContactUseCase() {
        assertNotNull(config.tenantContactUseCase(tenantContactRepo, tenantUserPort));
    }

    @Test
    void shouldCreateTenantUserDepartmentUseCase() {
        assertNotNull(config.tenantUserDepartmentUseCase(deptRepo, tenantUserPort));
    }

    @Test
    void shouldCreateCoreHrAttendanceSummaryPort() {
        assertNotNull(config.coreHrAttendanceSummaryPort(jdbcTemplate));
    }

    @Test
    void shouldCreateEnrollService() {
        assertNotNull(config.enrollService(tenantUserPort, deviceRepo, userBiometriaRepo, enrollSessionStore));
    }
}
