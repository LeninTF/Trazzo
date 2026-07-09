package trazzo.back.saasglobal.application.dto.command;

public record CreateTrialTenantCommand(
        String subDomain,
        Integer planId,
        Integer holdingId,
        String logoUrl,
        String slogan,
        String primaryColor,
        String secondaryColor
) {
}
