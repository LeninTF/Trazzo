package trazzo.back.incidents.application.dto.command;

public record CreateIncidentCommand(Integer tenantUserId, Integer incidentTypeId, String comment) {
}
