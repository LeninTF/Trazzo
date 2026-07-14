package trazzo.back.saasglobal.infrastructure.adapters.out.mercadopago;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preapproval.PreapprovalClient;
import com.mercadopago.client.preapproval.PreapprovalCreateRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preapproval.Preapproval;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PaymentDetails;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalCreated;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalDetails;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalRequest;

@ExtendWith(MockitoExtension.class)
class MercadoPagoSubscriptionAdapterTest {

    @Mock PreapprovalClient preapprovalClient;
    @Mock PaymentClient mercadoPagoPaymentClient;
    @InjectMocks MercadoPagoSubscriptionAdapter adapter;

    @Test
    void createPreapproval_returnsCreatedDetails() throws Exception {
        Preapproval created = new Preapproval();
        setField(created, "id", "preapproval-1");
        setField(created, "status", "pending");
        setField(created, "initPoint", "https://mp.example/init");
        setField(created, "sandboxInitPoint", "https://mp.example/sandbox-init");
        when(preapprovalClient.create(any(PreapprovalCreateRequest.class))).thenReturn(created);

        PreapprovalCreated result = adapter.createPreapproval(new PreapprovalRequest(
                new BigDecimal("29.99"), "SOLES", "MONTHLY", "payer@test.com", "tenant-1",
                "https://front/gracias", "Suscripción Trazzo - Plan Demo"));

        assertEquals("preapproval-1", result.id());
        assertEquals("pending", result.status());
        assertEquals("https://mp.example/init", result.initPoint());
        assertEquals("https://mp.example/sandbox-init", result.sandboxInitPoint());
    }

    @Test
    void createPreapproval_wrapsMpApiException() throws Exception {
        var mockResponse = org.mockito.Mockito.mock(com.mercadopago.net.MPResponse.class);
        var mpApiException = new MPApiException("boom", mockResponse);
        when(preapprovalClient.create(any(PreapprovalCreateRequest.class))).thenThrow(mpApiException);

        assertThrows(MercadoPagoIntegrationException.class, () -> adapter.createPreapproval(new PreapprovalRequest(
                BigDecimal.TEN, "SOLES", "MONTHLY", "payer@test.com", "tenant-1", "back", "reason")));
    }

    @Test
    void getPreapproval_returnsDetails() throws Exception {
        Preapproval preapproval = new Preapproval();
        setField(preapproval, "id", "preapproval-1");
        setField(preapproval, "status", "authorized");
        setField(preapproval, "externalReference", "tenant-1");
        when(preapprovalClient.get("preapproval-1")).thenReturn(preapproval);

        PreapprovalDetails result = adapter.getPreapproval("preapproval-1");

        assertEquals("preapproval-1", result.id());
        assertEquals("authorized", result.status());
        assertEquals("tenant-1", result.externalReference());
    }

    @Test
    void getPayment_returnsDetails() throws Exception {
        Payment payment = new Payment();
        setField(payment, "id", 12345L);
        setField(payment, "status", "approved");
        setField(payment, "transactionAmount", new BigDecimal("29.99"));
        setField(payment, "netAmount", new BigDecimal("28.50"));
        setField(payment, "externalReference", "tenant-1");
        when(mercadoPagoPaymentClient.get(12345L)).thenReturn(payment);

        PaymentDetails result = adapter.getPayment("12345");

        assertEquals("12345", result.id());
        assertEquals("approved", result.status());
        assertEquals(new BigDecimal("29.99"), result.transactionAmount());
        assertEquals(new BigDecimal("28.50"), result.netAmount());
        assertEquals("tenant-1", result.externalReference());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
