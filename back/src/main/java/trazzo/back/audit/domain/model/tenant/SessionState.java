package trazzo.back.audit.domain.model.tenant;

public enum SessionState {
    ACTIVE,
    EXPIRED,
    LOGGED_OUT,
    REVOKED,
    BLOCKED
}
