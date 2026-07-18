package trazzo.back.saasglobal.domain.model.iam;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private String id;
    private Integer personId;
    private String tenantId;
    private String email;
    private String phone;
    private String password;
    private List<String> roles;
    private List<String> permissionCodes;
    private boolean mustChangePassword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    Clock clock = Clock.systemDefaultZone();

    @SuppressWarnings("java:S107")
    private User(String id, Integer personId, String tenantId, String email, String phone,
                 String password, List<String> roles, List<String> permissionCodes, boolean mustChangePassword,
                 LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.personId = requireNonNull(personId, "personId");
        this.tenantId = tenantId;
        this.email = requireEmail(email);
        this.phone = phone;
        this.password = requireText(password, "password");
        this.roles = roles != null ? List.copyOf(roles) : List.of();
        this.permissionCodes = permissionCodes != null ? List.copyOf(permissionCodes) : List.of();
        this.mustChangePassword = mustChangePassword;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static User create(Integer personId, String tenantId, String email,
                              String phone, String encodedPassword) {
        return create(personId, tenantId, email, phone, encodedPassword, false);
    }

    public static User create(Integer personId, String tenantId, String email,
                              String phone, String encodedPassword, boolean mustChangePassword) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new User(null, personId, tenantId, email, phone, encodedPassword,
                List.of(), List.of(), mustChangePassword, now, now, null);
    }

    @SuppressWarnings("java:S107")
    public static User restore(String id, Integer personId, String tenantId, String email,
                               String phone, String password, List<String> roles, List<String> permissionCodes,
                               boolean mustChangePassword, LocalDateTime createdAt, LocalDateTime updatedAt,
                               LocalDateTime deletedAt) {
        return new User(id, personId, tenantId, email, phone, password, roles, permissionCodes,
                mustChangePassword, createdAt, updatedAt, deletedAt);
    }

    public void updateContact(String email, String phone) {
        this.email = requireEmail(email);
        this.phone = phone;
        this.updatedAt = LocalDateTime.now(clock);
    }

    public void clearMustChangePassword() {
        this.mustChangePassword = false;
        this.updatedAt = LocalDateTime.now(clock);
    }

    public void delete() {
        if (!isActive()) {
            throw new UserValidationException("user is already deleted");
        }
        this.deletedAt = LocalDateTime.now(clock);
        this.updatedAt = this.deletedAt;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    private static String requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new UserValidationException("email is required");
        }
        if (!email.contains("@")) {
            throw new UserValidationException("email format is invalid");
        }
        return email.trim().toLowerCase();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UserValidationException(fieldName + " is required");
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new UserValidationException(fieldName + " is required");
        }
        return value;
    }
}
