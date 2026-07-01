package trazzo.back.corehr.application.dto.result;

import java.time.LocalDateTime;

public record TenantContactResult(
        Long id,
        Long tenantUserId,
        String type,
        TenantUserBasicInfo tenantUser,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public record TenantUserBasicInfo(Long id, String nombre, String apellidoPaterno,
                                      String apellidoMaterno, String email, String phone) {
    }
}
