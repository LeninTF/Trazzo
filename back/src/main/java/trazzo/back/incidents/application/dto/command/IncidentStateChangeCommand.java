package trazzo.back.incidents.application.dto.command;

import trazzo.back.incidents.domain.model.IncidentState;

public record IncidentStateChangeCommand(IncidentState state, Integer daysGranted, String motivoRechazo) {
}
