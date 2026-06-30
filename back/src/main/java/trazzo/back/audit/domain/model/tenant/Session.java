package trazzo.back.audit.domain.model.tenant;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class Session {
    private Long id;
    private String tenantUserId;
    private String refreshTokenHash;
    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;
    private LocalDateTime loginAt;
    private LocalDateTime lasActivityAt;
    private LocalDateTime logoutAt;
    private LocalDateTime expiresAt;
    private SessionState state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Session(
            Long id,
            String tenantUserId,
            String refreshTokenHash,
            String ipAddress,
            String userAgent,
            String deviceFingerprint,
            LocalDateTime loginAt,
            LocalDateTime lasActivityAt,
            LocalDateTime logoutAt,
            LocalDateTime expiresAt,
            SessionState state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        if (tenantUserId == null || tenantUserId.isBlank()) {
            throw new IllegalArgumentException("Tenant user id is required.");
        }
        if (refreshTokenHash == null || refreshTokenHash.isBlank()) {
            throw new IllegalArgumentException("Refresh token hash is required.");
        }
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException("IP address is required.");
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("User agent is required.");
        }
        if (loginAt == null) {
            throw new IllegalArgumentException("Login date is required.");
        }
        if (expiresAt.isBefore(loginAt)) {
            throw new IllegalArgumentException("Expiration date cannot be before login date.");
        }
        if (!expiresAt.isAfter(loginAt)) {
            throw new IllegalArgumentException("Expiration date must be after login date.");
        }
        if (logoutAt != null && logoutAt.isBefore(loginAt)) {
            throw new IllegalArgumentException("Logout date cannot be before login date.");
        }
        if (lasActivityAt != null && lasActivityAt.isBefore(loginAt)) {
            throw new IllegalArgumentException("Last activity cannot be before login.");
        }
        if (state == null) {
            throw new IllegalArgumentException("Session state is required.");
        }
        if (state == SessionState.LOGGED_OUT && logoutAt == null) {
            throw new IllegalArgumentException("Logged out session must have logout date.");
        }
        if (state == SessionState.ACTIVE && logoutAt != null) {
            throw new IllegalArgumentException("Active session cannot have logout date.");
        }
        if (state == SessionState.EXPIRED &&
    expiresAt.isAfter(LocalDateTime.now())) {

    throw new IllegalArgumentException("Session is not expired yet.");
}
        this.id = id;
        this.tenantUserId = tenantUserId;
        this.refreshTokenHash = refreshTokenHash;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceFingerprint = deviceFingerprint;
        this.loginAt = loginAt;
        this.lasActivityAt = lasActivityAt;
        this.logoutAt = logoutAt;
        this.expiresAt = expiresAt;
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}