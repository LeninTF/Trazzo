package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.command.CreateTenantUserDepartmentCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantUserDepartmentCommand;
import trazzo.back.corehr.application.dto.result.TenantUserDepartmentResult;
import trazzo.back.corehr.application.port.in.TenantUserDepartmentUseCase;
import trazzo.back.corehr.application.port.out.TenantUserDepartmentRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.domain.model.employee.TenantUserDepartment;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
public class TenantUserDepartmentService implements TenantUserDepartmentUseCase {

    private final TenantUserDepartmentRepositoryPort departmentRepository;
    private final TenantUserPort tenantUserPort;

    @Override
    public TenantUserDepartmentResult create(Long tenantUserId, CreateTenantUserDepartmentCommand command) {
        var tenantUserIdStr = String.valueOf(tenantUserId);
        if (!tenantUserPort.existsById(tenantUserIdStr)) {
            throw new IllegalArgumentException("TenantUser no encontrado: " + tenantUserId);
        }
        if (command.isPrimary()) {
            var existingPrimary = departmentRepository.findPrimaryByTenantUserId(tenantUserId);
            existingPrimary.ifPresent(primary -> {
                primary.endAssignment(command.startDate().minusDays(1));
                departmentRepository.save(primary);
            });
        }
        var dept = TenantUserDepartment.create(
                tenantUserId, command.departmentId(), command.isPrimary(),
                command.startDate(), command.endDate()
        );
        var saved = departmentRepository.save(dept);
        return toResult(saved);
    }

    @Override
    public List<TenantUserDepartmentResult> findAllByTenantUserId(Long tenantUserId) {
        if (!tenantUserPort.existsById(String.valueOf(tenantUserId))) {
            throw new IllegalArgumentException("TenantUser no encontrado: " + tenantUserId);
        }
        return departmentRepository.findAllByTenantUserId(tenantUserId)
                .stream().map(this::toResult).toList();
    }

    @Override
    public TenantUserDepartmentResult patch(Long tenantUserId, Long departamentoId, PatchTenantUserDepartmentCommand command) {
        var dept = departmentRepository.findByTenantUserIdAndDepartmentId(tenantUserId, departamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));
        if (command.endDate() != null) {
            dept.endAssignment(command.endDate());
        }
        if (command.isPrimary() != null) {
            if (command.isPrimary()) {
                dept.markAsPrimary();
            } else {
                dept.unmarkAsPrimary();
            }
        }
        var saved = departmentRepository.save(dept);
        return toResult(saved);
    }

    private TenantUserDepartmentResult toResult(TenantUserDepartment dept) {
        return new TenantUserDepartmentResult(
                dept.getId(),
                dept.getTenantUserId(),
                dept.getDepartmentId(),
                null,
                dept.isPrimary(),
                dept.getStartDate(),
                dept.getEndDate(),
                dept.getCreatedAt(),
                dept.getUpdatedAt()
        );
    }
}
