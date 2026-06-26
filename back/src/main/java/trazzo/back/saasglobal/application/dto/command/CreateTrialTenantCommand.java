package trazzo.back.saasglobal.application.dto.command;

public record CreateTrialTenantCommand(
        String subDomain,
        Integer planId,
        Integer holdingId,
        String dbHost,
        String dbPort,
        String dbName,
        String dbUser,
        String dbPassword,
        String logoUrl,
        String slogan,
        String primaryColor,
        String secondaryColor
) {
}
