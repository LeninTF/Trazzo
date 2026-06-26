package trazzo.back.incidents.domain.specification;

import trazzo.back.incidents.domain.model.IncidentState;

public class IncidentStateTransitionSpec {

    public boolean canTransition(IncidentState currentState, IncidentState targetState) {
        return currentState == IncidentState.PENDIENTE
                && (targetState == IncidentState.APROBADO || targetState == IncidentState.DENEGADO);
    }
}
