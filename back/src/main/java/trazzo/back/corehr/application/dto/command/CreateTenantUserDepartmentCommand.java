package trazzo.back.corehr.application.dto.command;

import java.time.LocalDate;

public record CreateTenantUserDepartmentCommand(Long departmentId, boolean isPrimary,
                                                LocalDate startDate, LocalDate endDate) {
}
