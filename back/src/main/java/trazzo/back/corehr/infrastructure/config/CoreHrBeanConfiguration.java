package trazzo.back.corehr.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.corehr.application.port.in.CoreHrAttendanceSummaryPort;
import trazzo.back.corehr.application.port.out.AttendanceRepositoryPort;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.application.port.out.EventPublisherPort;
import trazzo.back.corehr.application.port.out.NonWorkingDaysRepositoryPort;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.application.port.out.ShiftRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantContactRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserDepartmentRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.application.port.out.ToleranciaRepositoryPort;
import trazzo.back.corehr.application.port.out.UserBiometriaRepositoryPort;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.application.usecase.AttendanceService;
import trazzo.back.corehr.application.usecase.DeviceService;
import trazzo.back.corehr.application.usecase.NonWorkingDayService;
import trazzo.back.corehr.application.usecase.ScheduleService;
import trazzo.back.corehr.application.usecase.ShiftService;
import trazzo.back.corehr.application.usecase.TenantContactService;
import trazzo.back.corehr.application.usecase.TenantUserDepartmentService;
import trazzo.back.corehr.application.usecase.ToleranciaService;
import trazzo.back.corehr.application.usecase.UserBiometriaService;
import trazzo.back.corehr.application.usecase.UserScheduleService;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollService;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollSessionStore;
import trazzo.back.corehr.infrastructure.adapters.out.reporting.CoreHrAttendanceSummaryJdbcAdapter;

@Configuration
public class CoreHrBeanConfiguration {

    @Bean
    public ShiftService shiftUseCase(ShiftRepositoryPort shiftRepo, ScheduleRepositoryPort scheduleRepo) {
        return new ShiftService(shiftRepo, scheduleRepo);
    }

    @Bean
    public ScheduleService scheduleUseCase(ScheduleRepositoryPort scheduleRepo, ShiftRepositoryPort shiftRepo,
                                           ToleranciaRepositoryPort toleranciaRepo, UserScheduleRepositoryPort userScheduleRepo) {
        return new ScheduleService(scheduleRepo, shiftRepo, toleranciaRepo, userScheduleRepo);
    }

    @Bean
    public ToleranciaService toleranciaUseCase(ToleranciaRepositoryPort toleranciaRepo, ScheduleRepositoryPort scheduleRepo) {
        return new ToleranciaService(toleranciaRepo, scheduleRepo);
    }

    @Bean
    public UserScheduleService userScheduleUseCase(UserScheduleRepositoryPort userScheduleRepo,
                                                   ScheduleRepositoryPort scheduleRepo, TenantUserPort tenantUserPort) {
        return new UserScheduleService(userScheduleRepo, scheduleRepo, tenantUserPort);
    }

    @Bean
    public DeviceService deviceUseCase(DeviceRepositoryPort deviceRepo) {
        return new DeviceService(deviceRepo);
    }

    @Bean
    public UserBiometriaService userBiometriaUseCase(UserBiometriaRepositoryPort userBiometriaRepo, EnrollService enrollService) {
        return new UserBiometriaService(userBiometriaRepo, enrollService);
    }

    @Bean
    public AttendanceService attendanceUseCase(AttendanceRepositoryPort attendanceRepo, EventPublisherPort eventPublisher) {
        return new AttendanceService(attendanceRepo, eventPublisher);
    }

    @Bean
    public NonWorkingDayService nonWorkingDayUseCase(NonWorkingDaysRepositoryPort nonWorkingDaysRepo) {
        return new NonWorkingDayService(nonWorkingDaysRepo);
    }

    @Bean
    public TenantContactService tenantContactUseCase(TenantContactRepositoryPort tenantContactRepo, TenantUserPort tenantUserPort) {
        return new TenantContactService(tenantContactRepo, tenantUserPort);
    }

    @Bean
    public TenantUserDepartmentService tenantUserDepartmentUseCase(TenantUserDepartmentRepositoryPort deptRepo, TenantUserPort tenantUserPort) {
        return new TenantUserDepartmentService(deptRepo, tenantUserPort);
    }

    @Bean
    public EnrollService enrollService(TenantUserPort tenantUserPort, DeviceRepositoryPort deviceRepo,
                                       UserBiometriaRepositoryPort userBiometriaRepo, EnrollSessionStore enrollSessionStore) {
        return new EnrollService(tenantUserPort, deviceRepo, userBiometriaRepo, enrollSessionStore);
    }

    @Bean
    public CoreHrAttendanceSummaryPort coreHrAttendanceSummaryPort(JdbcTemplate jdbcTemplate) {
        return new CoreHrAttendanceSummaryJdbcAdapter(jdbcTemplate);
    }
}
