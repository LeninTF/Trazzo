package trazzo.back.saasglobal.domain.model.iam;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Person {

    private Integer id;
    private String imgUrl;
    private DocumentType documentType;
    private String documentValue;
    private String name;
    private String fatherSurname;
    private String motherSurname;
    private LocalDate birthDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @SuppressWarnings("java:S107")
    private Person(Integer id, String imgUrl, DocumentType documentType, String documentValue,
                   String name, String fatherSurname, String motherSurname, LocalDate birthDate,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.imgUrl = imgUrl;
        this.documentType = requireNonNull(documentType, "documentType");
        this.documentValue = requireText(documentValue, "documentValue");
        this.name = requireText(name, "name");
        this.fatherSurname = requireText(fatherSurname, "fatherSurname");
        this.motherSurname = requireText(motherSurname, "motherSurname");
        this.birthDate = birthDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Person create(String imgUrl, DocumentType documentType, String documentValue,
                                String name, String fatherSurname, String motherSurname,
                                LocalDate birthDate) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new Person(null, imgUrl, documentType, documentValue,
                name, fatherSurname, motherSurname, birthDate, now, now);
    }

    @SuppressWarnings("java:S107")
    public static Person restore(Integer id, String imgUrl, DocumentType documentType,
                                 String documentValue, String name, String fatherSurname,
                                 String motherSurname, LocalDate birthDate,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Person(id, imgUrl, documentType, documentValue,
                name, fatherSurname, motherSurname, birthDate, createdAt, updatedAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UserValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new UserValidationException(fieldName + " is required");
        }
        return value;
    }
}
