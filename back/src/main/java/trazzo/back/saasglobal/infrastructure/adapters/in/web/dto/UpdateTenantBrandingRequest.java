package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

public record UpdateTenantBrandingRequest(
        String logoUrl,
        String slogan,
        String primaryColor,
        String secondaryColor
) {}
