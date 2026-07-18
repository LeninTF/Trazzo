package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import trazzo.back.saasglobal.domain.model.invoice.Invoice;

@ExtendWith(MockitoExtension.class)
class InvoiceJdbcRepositoryAdapterTest {

    @Mock NamedParameterJdbcTemplate namedJdbc;
    @InjectMocks InvoiceJdbcRepositoryAdapter adapter;

    @Test
    @SuppressWarnings("unchecked")
    void findByFilters_returnsEmptyListWhenNoMatches() {
        when(namedJdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        List<Invoice> result = adapter.findByFilters(null, null, null, null, 0, 20);

        assertTrue(result.isEmpty());
    }

    @Test
    void countByFilters_returnsZeroWhenNull() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(null);

        assertEquals(0L, adapter.countByFilters(null, null, null, null));
    }

    @Test
    void countByFilters_returnsCount() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(3L);

        assertEquals(3L, adapter.countByFilters("PENDIENTE", null, null, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAllMatching_returnsMappedInvoices() throws Exception {
        var now = LocalDateTime.now();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("inv-1");
        when(rs.getString("pdf_url")).thenReturn(null);
        when(rs.getString("tenant_id")).thenReturn("tenant-1");
        when(rs.getString("payment_transaction_id")).thenReturn(null);
        when(rs.getString("invoice_series")).thenReturn("F001");
        when(rs.getString("consecutive_number")).thenReturn("001");
        when(rs.getString("type")).thenReturn("01_FACTURA");
        when(rs.getString("issuer_tax_id")).thenReturn("20111111111");
        when(rs.getString("issuer_name")).thenReturn("Trazzo SAC");
        when(rs.getString("issuer_tax_address")).thenReturn("Av. Lima");
        when(rs.getString("client_tax_id")).thenReturn("20222222222");
        when(rs.getString("client_name")).thenReturn("Cliente SAC");
        when(rs.getString("client_direccion")).thenReturn(null);
        when(rs.getString("currency_code")).thenReturn("PEN");
        when(rs.getBigDecimal("exchange_rate")).thenReturn(null);
        when(rs.getBigDecimal("sub_total")).thenReturn(java.math.BigDecimal.TEN);
        when(rs.getBigDecimal("igv_amount")).thenReturn(java.math.BigDecimal.ONE);
        when(rs.getBigDecimal("total")).thenReturn(java.math.BigDecimal.TEN);
        when(rs.getBigDecimal("descuento_total")).thenReturn(null);
        when(rs.getString("estado_pago")).thenReturn("PENDIENTE");
        when(rs.getString("observaciones")).thenReturn(null);
        when(rs.getObject("expiration_date", LocalDate.class)).thenReturn(null);
        when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(now);

        when(namedJdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(inv -> {
                    RowMapper<Invoice> mapper = inv.getArgument(2);
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<Invoice> result = adapter.findAllMatching(null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("F001", result.get(0).getInvoiceSeries());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsEmptyWhenNotFound() {
        when(namedJdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        Optional<Invoice> result = adapter.findById("missing");

        assertTrue(result.isEmpty());
    }
}
