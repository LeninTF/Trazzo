package trazzo.back.incidents.application.dto.command;

public record CreateIncidentCommand(String tenantUserId, String incidentTypeId, String comment) {
}
