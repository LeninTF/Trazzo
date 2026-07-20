package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

public record ChangePasswordRequest(
    String currentPassword,
    String newPassword
) {}
