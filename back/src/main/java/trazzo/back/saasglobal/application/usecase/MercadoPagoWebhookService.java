package trazzo.back.saasglobal.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.dto.command.MercadoPagoWebhookCommand;
import trazzo.back.saasglobal.application.port.in.ProcessMercadoPagoWebhookUseCase;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PaymentDetails;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalDetails;
import trazzo.back.saasglobal.application.port.out.PaymentTransactionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PaymentWebhooksLogRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.domain.model.invoice.PaymentTransaction;
import trazzo.back.saasglobal.domain.model.invoice.PaymentWebhooksLog;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;

/**
 * Mercado Pago requires a 200/201 within 22 seconds or it retries — business-logic failures
 * (missing subscription, unreachable MP API for a stale preapproval, etc.) are logged and
 * swallowed here rather than propagated, so a permanently-unresolvable event doesn't trigger
 * endless retries. Only signature validation (done in the controller, before this is called)
 * rejects a request outright.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MercadoPagoWebhookService implements ProcessMercadoPagoWebhookUseCase {

    private static final String TYPE_PREAPPROVAL = "subscription_preapproval";
    private static final String TYPE_AUTHORIZED_PAYMENT = "subscription_authorized_payment";

    private final PaymentWebhooksLogRepositoryPort webhooksLogRepository;
    private final MercadoPagoSubscriptionPort mercadoPagoSubscriptionPort;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final PaymentTransactionRepositoryPort paymentTransactionRepository;
    private final PlanRepositoryPort planRepository;
    private final TenantRepositoryPort tenantRepository;

    @Override
    @Transactional
    public void process(MercadoPagoWebhookCommand cmd) {
        boolean shouldProcess = webhooksLogRepository.insertOrShouldRetry(
                PaymentWebhooksLog.create(cmd.notificationId(), cmd.notificationId(), cmd.action(), cmd.rawPayload()));
        if (!shouldProcess) {
            log.info("Mercado Pago webhook notificationId={} already processed, skipping", cmd.notificationId());
            return;
        }
        try {
            dispatch(cmd);
            webhooksLogRepository.markProcessed(cmd.notificationId());
        } catch (RuntimeException e) {
            // Left unprocessed on purpose: a Mercado Pago retry (or manual replay) will find
            // insertOrShouldRetry() true again and re-attempt dispatch(), instead of a
            // transient failure permanently dropping this payment/subscription update.
            log.error("Failed to process Mercado Pago webhook notificationId={} type={}, will retry on next delivery",
                    cmd.notificationId(), cmd.type(), e);
        }
    }

    private void dispatch(MercadoPagoWebhookCommand cmd) {
        if (cmd.dataId() == null) {
            log.warn("Mercado Pago webhook notificationId={} type={} has no data.id, ignoring",
                    cmd.notificationId(), cmd.type());
            return;
        }
        if (TYPE_PREAPPROVAL.equals(cmd.type())) {
            handlePreapprovalEvent(cmd.dataId());
        } else if (TYPE_AUTHORIZED_PAYMENT.equals(cmd.type())) {
            handleAuthorizedPaymentEvent(cmd.dataId());
        } else {
            log.info("Mercado Pago webhook notificationId={} type={} not handled, ignoring",
                    cmd.notificationId(), cmd.type());
        }
    }

    private void handlePreapprovalEvent(String preapprovalId) {
        PreapprovalDetails details = mercadoPagoSubscriptionPort.getPreapproval(preapprovalId);
        subscriptionRepository.findByMpPreapprovalId(preapprovalId).ifPresentOrElse(subscription -> {
            if ("authorized".equalsIgnoreCase(details.status()) && subscription.isTrial()) {
                subscription.activate(computeDateEnd(subscription.getPlanId()));
                subscriptionRepository.save(subscription);
                syncTenantPlan(subscription.getTenantId(), subscription.getPlanId());
            }
        }, () -> log.warn("No subscription found for mp_preapproval_id={}", preapprovalId));
    }

    private void syncTenantPlan(String tenantId, Integer planId) {
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            if (!planId.equals(tenant.getPlanId())) {
                tenant.changePlan(planId);
                tenantRepository.save(tenant);
            }
        });
    }

    private void handleAuthorizedPaymentEvent(String paymentId) {
        if (paymentTransactionRepository.findByMpPaymentId(paymentId).isPresent()) {
            log.info("Payment mp_payment_id={} already recorded, skipping", paymentId);
            return;
        }
        PaymentDetails details = mercadoPagoSubscriptionPort.getPayment(paymentId);
        String tenantId = details.externalReference();
        if (tenantId == null) {
            log.warn("Mercado Pago payment id={} has no external_reference, cannot resolve tenant", paymentId);
            return;
        }
        subscriptionRepository.findActiveByTenantId(tenantId).ifPresentOrElse(subscription -> {
            PaymentTransaction tx = PaymentTransaction.create(
                    tenantId, subscription.getId(), null, details.transactionAmount(), details.netAmount());
            if ("approved".equalsIgnoreCase(details.status())) {
                tx.approve(details.id());
            } else if ("rejected".equalsIgnoreCase(details.status())) {
                tx.reject();
            }
            paymentTransactionRepository.save(tx);
        }, () -> log.warn("No active subscription found for tenantId={}", tenantId));
    }

    private LocalDate computeDateEnd(Integer planId) {
        Plan plan = planRepository.findById(planId).orElse(null);
        boolean annual = plan != null && isAnnualBillingPeriod(plan.getBillingPeriod());
        return LocalDate.now(Clock.systemDefaultZone()).plusMonths(annual ? 12 : 1);
    }

    private static boolean isAnnualBillingPeriod(String billingPeriod) {
        if (billingPeriod == null) {
            return false;
        }
        String upper = billingPeriod.toUpperCase();
        return upper.contains("ANNUAL") || upper.contains("ANUAL") || upper.contains("YEARLY");
    }
}
