package trazzo.back.audit.domain.model.master;

public enum StatusLogin {
    SUCCESS,
    FAILED_WRONG_PASSWORD,
    FAILED_USER_NOT_FOUND,
    FAILED_INACTIVE_USER,
    LOCKED_OUT,
    LOGOUT_EXPLICIT
}