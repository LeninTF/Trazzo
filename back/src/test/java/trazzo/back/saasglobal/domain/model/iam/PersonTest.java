package trazzo.back.saasglobal.domain.model.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

class PersonTest {

    @Test
    void create_setsAllFields() {
        var p = Person.create(null, DocumentType.DNI, "12345678",
                "Juan", "Pérez", "García", LocalDate.of(1990, 1, 1));

        assertThat(p.getDocumentType()).isEqualTo(DocumentType.DNI);
        assertThat(p.getDocumentValue()).isEqualTo("12345678");
        assertThat(p.getName()).isEqualTo("Juan");
        assertThat(p.getFatherSurname()).isEqualTo("Pérez");
        assertThat(p.getMotherSurname()).isEqualTo("García");
        assertThat(p.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(p.getId()).isNull();
        assertThat(p.getCreatedAt()).isNotNull();
    }

    @Test
    void restore_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        var p = Person.restore(1, "http://img.jpg", DocumentType.PASAPORTE, "AB123",
                "Ana", "López", "Torres", LocalDate.of(1985, 6, 15), now, now);

        assertThat(p.getId()).isEqualTo(1);
        assertThat(p.getImgUrl()).isEqualTo("http://img.jpg");
        assertThat(p.getDocumentType()).isEqualTo(DocumentType.PASAPORTE);
        assertThat(p.getDocumentValue()).isEqualTo("AB123");
    }

    @Test
    void create_throwsWhenDocumentTypeNull() {
        assertThatThrownBy(() ->
                Person.create(null, null, "12345678", "Juan", "Pérez", "García", null))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("documentType");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void create_throwsWhenDocumentValueBlank(String value) {
        assertThatThrownBy(() ->
                Person.create(null, DocumentType.DNI, value, "Juan", "Pérez", "García", null))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("documentValue");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_throwsWhenNameBlank(String name) {
        assertThatThrownBy(() ->
                Person.create(null, DocumentType.DNI, "12345678", name, "Pérez", "García", null))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("name");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_throwsWhenFatherSurnameBlank(String surname) {
        assertThatThrownBy(() ->
                Person.create(null, DocumentType.DNI, "12345678", "Juan", surname, "García", null))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("fatherSurname");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_throwsWhenMotherSurnameBlank(String surname) {
        assertThatThrownBy(() ->
                Person.create(null, DocumentType.DNI, "12345678", "Juan", "Pérez", surname, null))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("motherSurname");
    }
}
