package trazzo.back.incidents.domain.specification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.domain.model.IncidentState;

class IncidentStateTransitionSpecTest {

    private final IncidentStateTransitionSpec spec = new IncidentStateTransitionSpec();

    @Test
    void canTransitionFromPendingToApproved() {
        assertTrue(spec.canTransition(IncidentState.PENDIENTE, IncidentState.APROBADO));
    }

    @Test
    void canTransitionFromPendingToDenied() {
        assertTrue(spec.canTransition(IncidentState.PENDIENTE, IncidentState.DENEGADO));
    }

    @Test
    void cannotTransitionFromApprovedToAny() {
        assertFalse(spec.canTransition(IncidentState.APROBADO, IncidentState.DENEGADO));
        assertFalse(spec.canTransition(IncidentState.APROBADO, IncidentState.PENDIENTE));
    }

    @Test
    void cannotTransitionFromDeniedToAny() {
        assertFalse(spec.canTransition(IncidentState.DENEGADO, IncidentState.APROBADO));
        assertFalse(spec.canTransition(IncidentState.DENEGADO, IncidentState.PENDIENTE));
    }
}
