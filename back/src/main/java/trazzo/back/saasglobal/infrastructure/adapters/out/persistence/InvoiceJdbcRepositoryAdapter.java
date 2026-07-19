package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.InvoiceRepositoryPort;
import trazzo.back.saasglobal.domain.model.invoice.Invoice;

@Repository
@RequiredArgsConstructor
public class InvoiceJdbcRepositoryAdapter implements InvoiceRepositoryPort {

    private static final String FILTER_WHERE = """
            WHERE (:paymentStatus IS NULL OR estado_pago = CAST(:paymentStatus AS estado_pago_comprobante_enum))
              AND (:tenantId IS NULL OR tenant_id = CAST(:tenantId AS uuid))
              AND (:dateFrom IS NULL OR created_at >= CAST(:dateFrom AS date))
              AND (:dateTo IS NULL OR created_at < CAST(:dateTo AS date) + INTERVAL '1 day')
            """;

    private final NamedParameterJdbcTemplate namedJdbc;

    @Override
    public List<Invoice> findByFilters(String paymentStatus, String tenantId, LocalDate dateFrom, LocalDate dateTo,
                                        int page, int size) {
        MapSqlParameterSource params = filterParams(paymentStatus, tenantId, dateFrom, dateTo)
                .addValue("limit", size)
                .addValue("offset", Math.max(page, 0) * size);
        return namedJdbc.query(
                "SELECT * FROM invoices " + FILTER_WHERE + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset",
                params, this::mapRow);
    }

    @Override
    public long countByFilters(String paymentStatus, String tenantId, LocalDate dateFrom, LocalDate dateTo) {
        Long count = namedJdbc.queryForObject(
                "SELECT COUNT(*) FROM invoices " + FILTER_WHERE,
                filterParams(paymentStatus, tenantId, dateFrom, dateTo), Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public List<Invoice> findAllMatching(String paymentStatus, String tenantId, LocalDate dateFrom, LocalDate dateTo) {
        return namedJdbc.query(
                "SELECT * FROM invoices " + FILTER_WHERE + " ORDER BY created_at DESC",
                filterParams(paymentStatus, tenantId, dateFrom, dateTo), this::mapRow);
    }

    @Override
    public Optional<Invoice> findById(String id) {
        List<Invoice> rows = namedJdbc.query(
                "SELECT * FROM invoices WHERE id = CAST(:id AS uuid)",
                new MapSqlParameterSource("id", id), this::mapRow);
        return rows.stream().findFirst();
    }

    private static MapSqlParameterSource filterParams(String paymentStatus, String tenantId,
                                                        LocalDate dateFrom, LocalDate dateTo) {
        return new MapSqlParameterSource()
                .addValue("paymentStatus", paymentStatus, Types.VARCHAR)
                .addValue("tenantId", tenantId, Types.VARCHAR)
                .addValue("dateFrom", dateFrom, Types.DATE)
                .addValue("dateTo", dateTo, Types.DATE);
    }

    private Invoice mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Invoice.restore(
                rs.getString("id"),
                rs.getString("pdf_url"),
                rs.getString("tenant_id"),
                rs.getString("payment_transaction_id"),
                rs.getString("invoice_series"),
                rs.getString("consecutive_number"),
                rs.getString("type"),
                rs.getString("issuer_tax_id"),
                rs.getString("issuer_name"),
                rs.getString("issuer_tax_address"),
                rs.getString("client_tax_id"),
                rs.getString("client_name"),
                rs.getString("client_direccion"),
                rs.getString("currency_code"),
                rs.getBigDecimal("exchange_rate"),
                rs.getBigDecimal("sub_total"),
                rs.getBigDecimal("igv_amount"),
                rs.getBigDecimal("total"),
                rs.getBigDecimal("descuento_total"),
                rs.getString("estado_pago"),
                rs.getString("observaciones"),
                rs.getObject("expiration_date", LocalDate.class),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
