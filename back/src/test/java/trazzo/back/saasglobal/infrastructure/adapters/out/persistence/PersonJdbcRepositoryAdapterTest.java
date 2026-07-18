package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.saasglobal.domain.model.iam.DocumentType;
import trazzo.back.saasglobal.domain.model.iam.Person;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks PersonJdbcRepositoryAdapter adapter;

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsEmptyWhenNotFound() {
        doThrow(new EmptyResultDataAccessException(1))
                .when(jdbc).queryForObject(anyString(), any(RowMapper.class), any());

        Optional<Person> result = adapter.findById(999);

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_mapsFullRow() throws SQLException {
        ResultSet rs = mockResultSet(
                1, null, "DNI", "00000000", "Admin", "Trazzo", "Sistema",
                Date.valueOf(LocalDate.of(1990, 1, 15)),
                Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 0, 0)),
                Timestamp.valueOf(LocalDateTime.of(2025, 6, 1, 0, 0))
        );

        Person person = captureRowMapper().mapRow(rs, 0);

        assertThat(person.getId()).isEqualTo(1);
        assertThat(person.getImgUrl()).isNull();
        assertThat(person.getDocumentType()).isEqualTo(DocumentType.DNI);
        assertThat(person.getDocumentValue()).isEqualTo("00000000");
        assertThat(person.getName()).isEqualTo("Admin");
        assertThat(person.getFatherSurname()).isEqualTo("Trazzo");
        assertThat(person.getMotherSurname()).isEqualTo("Sistema");
        assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(person.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
        assertThat(person.getUpdatedAt()).isEqualTo(LocalDateTime.of(2025, 6, 1, 0, 0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_mapsNullBirthDateAndTimestamps() throws SQLException {
        ResultSet rs = mockResultSet(
                2, "http://img", "PASAPORTE", "X1234", "Ana", "Perez", "Lopez",
                null, null, null
        );

        Person person = captureRowMapper().mapRow(rs, 0);

        assertThat(person.getBirthDate()).isNull();
        assertThat(person.getCreatedAt()).isNull();
        assertThat(person.getUpdatedAt()).isNull();
        assertThat(person.getImgUrl()).isEqualTo("http://img");
        assertThat(person.getDocumentType()).isEqualTo(DocumentType.PASAPORTE);
    }

    @Test
    void deleteById_deletesById() {
        adapter.deleteById(1);

        verify(jdbc).update(anyString(), org.mockito.ArgumentMatchers.eq(1));
    }

    @SuppressWarnings("unchecked")
    private RowMapper<Person> captureRowMapper() {
        adapter.findById(1);
        ArgumentCaptor<RowMapper<Person>> captor = ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbc).queryForObject(anyString(), captor.capture(), any());
        return captor.getValue();
    }

    private ResultSet mockResultSet(
            Integer id, String imgUrl, String documentType, String documentValue,
            String name, String fatherSurname, String motherSurname,
            Date birthDate, Timestamp createdAt, Timestamp updatedAt
    ) throws SQLException {
        ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
        when(rs.getInt("id")).thenReturn(id);
        when(rs.getString("img_url")).thenReturn(imgUrl);
        when(rs.getString("document_type")).thenReturn(documentType);
        when(rs.getString("document_value")).thenReturn(documentValue);
        when(rs.getString("name")).thenReturn(name);
        when(rs.getString("father_surname")).thenReturn(fatherSurname);
        when(rs.getString("mother_surname")).thenReturn(motherSurname);
        when(rs.getDate("birth_date")).thenReturn(birthDate);
        when(rs.getTimestamp("created_at")).thenReturn(createdAt);
        when(rs.getTimestamp("updated_at")).thenReturn(updatedAt);
        return rs;
    }
}
