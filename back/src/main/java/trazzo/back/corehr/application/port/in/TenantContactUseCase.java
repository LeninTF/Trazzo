package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.command.CreateTenantContactCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantContactCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.TenantContactResult;

public interface TenantContactUseCase {
    TenantContactResult create(CreateTenantContactCommand command);
    PaginatedResult<TenantContactResult> findAll(int page, int size);
    TenantContactResult patch(Long id, PatchTenantContactCommand command);
    void deleteById(Long id);
}
