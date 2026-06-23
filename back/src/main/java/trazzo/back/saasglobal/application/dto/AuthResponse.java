package trazzo.back.saasglobal.application.dto;

public record AuthResponse(
        String token,
        Long tenantId,
        String nombre
) {}
