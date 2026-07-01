package trazzo.back.corehr.application.dto.command;

public record CreateTenantContactCommand(Long tenantUserId, String type) {
}
