package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.PersonRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.DocumentType;
import trazzo.back.saasglobal.domain.model.iam.Person;

@Repository
@RequiredArgsConstructor
public class PersonJdbcRepositoryAdapter implements PersonRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public Person save(Person person) {
        Integer id = jdbc.queryForObject(
                """
                INSERT INTO persons (img_url, document_type, document_value, name, father_surname,
                                     mother_surname, birth_date, created_at, updated_at)
                VALUES (?, ?::document_type_enum, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Integer.class,
                person.getImgUrl(), person.getDocumentType().name(), person.getDocumentValue(),
                person.getName(), person.getFatherSurname(), person.getMotherSurname(),
                person.getBirthDate(), person.getCreatedAt(), person.getUpdatedAt());
        return Person.restore(id, person.getImgUrl(), person.getDocumentType(), person.getDocumentValue(),
                person.getName(), person.getFatherSurname(), person.getMotherSurname(),
                person.getBirthDate(), person.getCreatedAt(), person.getUpdatedAt());
    }

    @Override
    public Optional<Person> findById(Integer id) {
        String sql = """
                SELECT id, img_url, document_type, document_value, name,
                       father_surname, mother_surname, birth_date, created_at, updated_at
                FROM persons
                WHERE id = ?
                """;
        try {
            Person person = jdbc.queryForObject(sql, new PersonRowMapper(), id);
            return Optional.ofNullable(person);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private static class PersonRowMapper implements RowMapper<Person> {
        @Override
        public Person mapRow(ResultSet rs, int rowNum) throws SQLException {
            LocalDate birthDate = rs.getDate("birth_date") != null
                    ? rs.getDate("birth_date").toLocalDate() : null;
            LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null;
            LocalDateTime updatedAt = rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null;
            return Person.restore(
                    rs.getInt("id"),
                    rs.getString("img_url"),
                    DocumentType.valueOf(rs.getString("document_type")),
                    rs.getString("document_value"),
                    rs.getString("name"),
                    rs.getString("father_surname"),
                    rs.getString("mother_surname"),
                    birthDate,
                    createdAt,
                    updatedAt
            );
        }
    }
}
