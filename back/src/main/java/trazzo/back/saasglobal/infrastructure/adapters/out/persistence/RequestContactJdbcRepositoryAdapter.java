package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.RequestContactRepositoryPort;
import trazzo.back.saasglobal.domain.model.request.RequestContact;

@Repository
@RequiredArgsConstructor
public class RequestContactJdbcRepositoryAdapter implements RequestContactRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public RequestContact save(RequestContact contact) {
        jdbc.update("""
                INSERT INTO request_contacts
                    (request_id, name, last_name, email, phone_number, tax_id, company_name, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                contact.getRequestId(),
                contact.getName(),
                contact.getLastName(),
                contact.getEmail(),
                contact.getPhoneNumber(),
                contact.getTaxId(),
                contact.getCompanyName(),
                contact.getCreatedAt(),
                contact.getUpdatedAt());
        return contact;
    }

    @Override
    public Optional<RequestContact> findByRequestId(Integer requestId) {
        List<RequestContact> rows = jdbc.query(
                "SELECT * FROM request_contacts WHERE request_id = ?", this::mapRow, requestId);
        return rows.stream().findFirst();
    }

    @Override
    public long countByTaxId(String taxId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM request_contacts WHERE tax_id = ?", Long.class, taxId);
        return count != null ? count : 0L;
    }

    @Override
    public boolean existsRecentByTaxId(String taxId, LocalDateTime since) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM request_contacts WHERE tax_id = ? AND created_at >= ?",
                Integer.class, taxId, since);
        return count != null && count > 0;
    }

    private RequestContact mapRow(ResultSet rs, int rowNum) throws SQLException {
        return RequestContact.restore(
                rs.getInt("request_id"),
                rs.getString("name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone_number"),
                rs.getString("tax_id"),
                rs.getString("company_name"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
