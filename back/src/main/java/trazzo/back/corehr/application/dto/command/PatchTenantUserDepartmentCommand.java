package trazzo.back.corehr.application.dto.command;

import java.time.LocalDate;

public record PatchTenantUserDepartmentCommand(LocalDate endDate, Boolean isPrimary) {
}
