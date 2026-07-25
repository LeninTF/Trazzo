package trazzo.back.corehr.application.port.out;

/**
 * Out port used by UserScheduleService to record every schedule assignment/unassignment
 * as an individual audit log entry. The implementation lives in the audit module's
 * infrastructure and writes N entries per bulk operation (one per tenant_user_id).
 *
 * <p>This port keeps corehr decoupled from audit's concrete persistence layer
 * (Hibernate entity / JdbcTemplate / whatever the audit module uses internally),
 * respecting the Dependency Rule of hexagonal architecture: the dependency arrow
 * always points inward (audit → corehr never happens).</p>
 */
public interface AuditScheduleAssignmentPort {

    enum Action {
        CREATE,
        DELETE
    }

    /**
     * Persist one audit entry describing that {@code scheduleId} was assigned to
     * (or removed from) {@code tenantUserId}. {@code userScheduleId} is the row id
     * of the {@code user_schedule} that triggered the event; {@code action}
     * discriminates between creation and removal.
     */
    void recordAssignment(Long tenantUserId, Long scheduleId, Long userScheduleId, Action action);
}
