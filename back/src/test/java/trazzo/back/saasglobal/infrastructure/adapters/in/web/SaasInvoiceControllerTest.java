package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.port.in.InvoiceUseCase;
import trazzo.back.saasglobal.application.port.out.InvoiceExcelExportPort;
import trazzo.back.saasglobal.application.port.out.InvoicePdfExportPort;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.SecurityConfig;

@WebMvcTest(SaasInvoiceController.class)
@Import(SecurityConfig.class)
class SaasInvoiceControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean InvoiceUseCase invoiceUseCase;
    @MockitoBean InvoiceExcelExportPort excelExportPort;
    @MockitoBean InvoicePdfExportPort pdfExportPort;

    private static UsernamePasswordAuthenticationToken authWithAuthorities(String... authorities) {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "admin@trazzo.pe", "pass", List.of(), true);
        var grants = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(authUser, null, grants);
    }

    private static InvoiceResult invoiceResult() {
        return new InvoiceResult("inv-1", "tenant-1", "F001", "001", "01_FACTURA",
                "20222222222", "Cliente SAC", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN,
                "PENDIENTE", null, LocalDateTime.now());
    }

    @Test
    void listAll_returns200_whenUserHasFacturacionPermission() throws Exception {
        when(invoiceUseCase.listAll(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(PaginatedResult.of(List.of(invoiceResult()), 0, 20, 1));

        mockMvc.perform(get("/saas/invoices")
                        .with(authentication(authWithAuthorities("ROLE_SAAS_ADMIN", "billing-suscripciones.historial-facturacion"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("inv-1"));
    }

    @Test
    void listAll_returns403_whenUserLacksFacturacionPermission() throws Exception {
        mockMvc.perform(get("/saas/invoices")
                        .with(authentication(authWithAuthorities("ROLE_SAAS_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAll_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/saas/invoices"))
                .andExpect(status().isUnauthorized());
    }
}
