package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import trazzo.back.saasglobal.application.dto.command.MercadoPagoWebhookCommand;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PaymentDetails;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalDetails;
import trazzo.back.saasglobal.application.port.out.PaymentTransactionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PaymentWebhooksLogRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;
import trazzo.back.saasglobal.domain.model.multitenancy.SubscriptionStatus;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MercadoPagoWebhookServiceTest {

    @Mock PaymentWebhooksLogRepositoryPort webhooksLogRepository;
    @Mock MercadoPagoSubscriptionPort mercadoPagoSubscriptionPort;
    @Mock SubscriptionRepositoryPort subscriptionRepository;
    @Mock PaymentTransactionRepositoryPort paymentTransactionRepository;
    @Mock PlanRepositoryPort planRepository;
    @Mock TenantRepositoryPort tenantRepository;
    @InjectMocks MercadoPagoWebhookService service;

    private static Tenant tenant(Integer planId) {
        return Tenant.restore("tenant-1", null, "acme", planId,
                TenantSettings.of("tenant-1", "tenant_acme"), null, LocalDateTime.now(), null,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void process_skipsWhenNotificationAlreadyLogged() {
        when(webhooksLogRepository.insertOrShouldRetry(any())).thenReturn(false);

        service.process(new MercadoPagoWebhookCommand("evt-1", "subscription_preapproval", "created", "pre-1", "{}"));

        verify(mercadoPagoSubscriptionPort, never()).getPreapproval(anyString());
        verify(webhooksLogRepository, never()).markProcessed(anyString());
    }

    @Test
    void process_activatesTrialSubscriptionWhenPreapprovalAuthorized() {
        when(webhooksLogRepository.insertOrShouldRetry(any())).thenReturn(true);
        when(mercadoPagoSubscriptionPort.getPreapproval("pre-1"))
                .thenReturn(new PreapprovalDetails("pre-1", "authorized", "tenant-1"));
        Subscription sub = Subscription.createTrial("tenant-1", 2, BigDecimal.ZERO, LocalDate.now());
        when(subscriptionRepository.findByMpPreapprovalId("pre-1")).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(2)).thenReturn(Optional.of(
                Plan.restore(2, "Plan Demo", BigDecimal.TEN, null, "SOLES", "MONTHLY", true, LocalDateTime.now(), null, null)));
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant(2)));

        service.process(new MercadoPagoWebhookCommand("evt-1", "subscription_preapproval", "created", "pre-1", "{}"));

        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        verify(webhooksLogRepository).markProcessed("evt-1");
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void process_updatesTenantPlanWhenActivatedSubscriptionIsForADifferentPlan() {
        when(webhooksLogRepository.insertOrShouldRetry(any())).thenReturn(true);
        when(mercadoPagoSubscriptionPort.getPreapproval("pre-2"))
                .thenReturn(new PreapprovalDetails("pre-2", "authorized", "tenant-1"));
        Subscription sub = Subscription.createTrial("tenant-1", 3, BigDecimal.ZERO, LocalDate.now());
        when(subscriptionRepository.findByMpPreapprovalId("pre-2")).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(3)).thenReturn(Optional.of(
                Plan.restore(3, "Plan Pro", BigDecimal.TEN, null, "SOLES", "MONTHLY", true, LocalDateTime.now(), null, null)));
        Tenant t = tenant(2);
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(t));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.process(new MercadoPagoWebhookCommand("evt-2", "subscription_preapproval", "updated", "pre-2", "{}"));

        assertEquals(3, t.getPlanId());
        verify(tenantRepository).save(t);
    }

    @Test
    void process_recordsApprovedPayment() {
        when(webhooksLogRepository.insertOrShouldRetry(any())).thenReturn(true);
        when(paymentTransactionRepository.findByMpPaymentId("pay-1")).thenReturn(Optional.empty());
        when(mercadoPagoSubscriptionPort.getPayment("pay-1")).thenReturn(
                new PaymentDetails("pay-1", "approved", new BigDecimal("29.99"), new BigDecimal("28.50"), "tenant-1"));
        Subscription sub = Subscription.createTrial("tenant-1", 2, BigDecimal.ZERO, LocalDate.now());
        when(subscriptionRepository.findActiveByTenantId("tenant-1")).thenReturn(Optional.of(sub));

        service.process(new MercadoPagoWebhookCommand("evt-3", "subscription_authorized_payment", "created", "pay-1", "{}"));

        var captor = org.mockito.ArgumentCaptor.forClass(
                trazzo.back.saasglobal.domain.model.invoice.PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        assertEquals("APPROVED", captor.getValue().getPaymentStatus());
        assertEquals("pay-1", captor.getValue().getMpPaymentId());
        verify(webhooksLogRepository).markProcessed("evt-3");
    }

    @Test
    void process_skipsAlreadyRecordedPayment() {
        when(webhooksLogRepository.insertOrShouldRetry(any())).thenReturn(true);
        when(paymentTransactionRepository.findByMpPaymentId("pay-1")).thenReturn(Optional.of(
                trazzo.back.saasglobal.domain.model.invoice.PaymentTransaction.create(
                        "tenant-1", "sub-1", null, BigDecimal.TEN, BigDecimal.TEN)));

        service.process(new MercadoPagoWebhookCommand("evt-4", "subscription_authorized_payment", "created", "pay-1", "{}"));

        verify(mercadoPagoSubscriptionPort, never()).getPayment(anyString());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void process_swallowsBusinessErrorsAndLeavesUnprocessedForRetry() {
        when(webhooksLogRepository.insertOrShouldRetry(any())).thenReturn(true);
        when(mercadoPagoSubscriptionPort.getPreapproval("pre-1"))
                .thenThrow(new RuntimeException("Mercado Pago unreachable"));

        assertDoesNotThrow(() -> service.process(
                new MercadoPagoWebhookCommand("evt-5", "subscription_preapproval", "created", "pre-1", "{}")));

        verify(webhooksLogRepository, never()).markProcessed(anyString());
    }

    @Test
    void process_ignoresUnknownEventType() {
        when(webhooksLogRepository.insertOrShouldRetry(any())).thenReturn(true);

        service.process(new MercadoPagoWebhookCommand("evt-6", "payment.created", "created", "res-1", "{}"));

        verify(mercadoPagoSubscriptionPort, never()).getPreapproval(anyString());
        verify(mercadoPagoSubscriptionPort, never()).getPayment(anyString());
        verify(webhooksLogRepository, times(1)).markProcessed("evt-6");
    }
}
