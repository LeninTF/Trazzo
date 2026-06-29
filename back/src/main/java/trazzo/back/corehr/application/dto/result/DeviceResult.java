package trazzo.back.corehr.application.dto.result;

import java.time.LocalDateTime;

public record DeviceResult(
        Long id,
        String code,
        String name,
        Long branchId,
        String branchName,
        String ip,
        Integer puerto,
        String ubicacion,
        boolean state,
        LocalDateTime createdAt
) {
}
