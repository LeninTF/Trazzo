package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.command.CreateTenantContactCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantContactCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.TenantContactResult;
import trazzo.back.corehr.application.port.in.TenantContactUseCase;
import trazzo.back.corehr.application.port.out.TenantContactRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.domain.model.employee.TenantContact;

@RequiredArgsConstructor
public class TenantContactService implements TenantContactUseCase {

    private final TenantContactRepositoryPort tenantContactRepository;
    private final TenantUserPort tenantUserPort;

    @Override
    public TenantContactResult create(CreateTenantContactCommand command) {
        if (!tenantUserPort.existsById(command.tenantUserId())) {
            throw new IllegalArgumentException("TenantUser no encontrado: " + command.tenantUserId());
        }
        var contact = TenantContact.create(command.tenantUserId(), command.type());
        var saved = tenantContactRepository.save(contact);
        return toResult(saved);
    }

    @Override
    public PaginatedResult<TenantContactResult> findAll(int page, int size) {
        var items = tenantContactRepository.findAll(page, size);
        var total = tenantContactRepository.count();
        var results = items.stream().map(this::toResult).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public TenantContactResult patch(Long id, PatchTenantContactCommand command) {
        var contact = tenantContactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contacto no encontrado: " + id));
        contact.updateType(command.type());
        var saved = tenantContactRepository.save(contact);
        return toResult(saved);
    }

    @Override
    public void deleteById(Long id) {
        var contact = tenantContactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contacto no encontrado: " + id));
        contact.markAsDeleted();
        tenantContactRepository.save(contact);
    }

    private TenantContactResult toResult(TenantContact contact) {
        var tenantUserInfo = tenantUserPort.findBasicInfoById(contact.getTenantUserId())
                .map(info -> new TenantContactResult.TenantUserBasicInfo(
                        info.id(), info.nombre(), info.apellidoPaterno(),
                        info.apellidoMaterno(), info.email(), info.phone()))
                .orElse(null);
        return new TenantContactResult(
                contact.getId(),
                contact.getTenantUserId(),
                contact.getType(),
                tenantUserInfo,
                contact.getCreatedAt(),
                contact.getUpdatedAt(),
                contact.getDeletedAt()
        );
    }
}
