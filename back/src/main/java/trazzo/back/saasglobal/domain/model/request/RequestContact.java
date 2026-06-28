package trazzo.back.saasglobal.domain.model.request;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestContact {

    private Integer requestId;
    private String name;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String taxId;
    private String companyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @SuppressWarnings("java:S107")
    private RequestContact(Integer requestId, String name, String lastName, String email,
                           String phoneNumber, String taxId, String companyName,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.requestId = requireNonNull(requestId, "requestId");
        this.name = requireText(name, "name");
        this.lastName = requireText(lastName, "lastName");
        this.email = requireText(email, "email");
        this.phoneNumber = requireText(phoneNumber, "phoneNumber");
        this.taxId = requireText(taxId, "taxId");
        this.companyName = requireText(companyName, "companyName");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @SuppressWarnings("java:S107")
    public static RequestContact create(Integer requestId, String name, String lastName,
                                        String email, String phoneNumber,
                                        String taxId, String companyName) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new RequestContact(requestId, name, lastName, email, phoneNumber, taxId, companyName, now, now);
    }

    @SuppressWarnings("java:S107")
    public static RequestContact restore(Integer requestId, String name, String lastName,
                                         String email, String phoneNumber, String taxId,
                                         String companyName, LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
        return new RequestContact(requestId, name, lastName, email, phoneNumber, taxId,
                companyName, createdAt, updatedAt);
    }

    private static String requireText(String v, String fieldName) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(fieldName + " is required");
        return v.trim();
    }

    private static <T> T requireNonNull(T v, String fieldName) {
        if (v == null) throw new IllegalArgumentException(fieldName + " is required");
        return v;
    }
}
