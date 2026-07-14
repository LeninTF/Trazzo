package trazzo.back.saasglobal.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.dto.command.SubscribeToPlanCommand;
import trazzo.back.saasglobal.application.dto.result.SubscribeToPlanResult;
import trazzo.back.saasglobal.application.port.in.SubscribeToPlanUseCase;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalCreated;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalRequest;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.domain.exception.InvalidSubscriptionTransitionException;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;

/**
 * A tenant changing or renewing their plan always gets a fresh preapproval and a fresh TRIAL
 * subscription record (activated by the webhook once Mercado Pago confirms it) — any subscription
 * that was still TRIAL/ACTIVE is canceled up front so a tenant never has two live subscriptions
 * at once; MercadoPagoWebhookService applies Tenant.changePlan() once the new one activates.
 */
@Service
@RequiredArgsConstructor
public class TenantPlanSubscriptionService implements SubscribeToPlanUseCase {

    private final PlanRepositoryPort planRepository;
    private final TenantRepositoryPort tenantRepository;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final MercadoPagoSubscriptionPort mercadoPagoSubscriptionPort;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    @Transactional
    public SubscribeToPlanResult subscribe(SubscribeToPlanCommand cmd) {
        Plan plan = planRepository.findById(cmd.planId())
                .filter(Plan::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or inactive: " + cmd.planId()));
        Tenant tenant = tenantRepository.findById(cmd.tenantId())
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + cmd.tenantId()));

        cancelCurrentSubscriptionIfAny(tenant.getId());

        Subscription subscription = Subscription.createTrial(tenant.getId(), plan.getId(), BigDecimal.ZERO, LocalDate.now());
        subscriptionRepository.save(subscription);

        PreapprovalCreated preapproval = mercadoPagoSubscriptionPort.createPreapproval(new PreapprovalRequest(
                plan.getPrice(),
                plan.getCurrency(),
                plan.getBillingPeriod(),
                cmd.payerEmail(),
                tenant.getId(),
                frontendUrl + "/org/billing",
                "Suscripción Trazzo - " + plan.getName()));

        subscription.linkMercadoPago(preapproval.id());
        subscriptionRepository.save(subscription);

        String redirectUrl = preapproval.sandboxInitPoint() != null
                ? preapproval.sandboxInitPoint() : preapproval.initPoint();
        return new SubscribeToPlanResult(subscription.getId(), redirectUrl);
    }

    private void cancelCurrentSubscriptionIfAny(String tenantId) {
        subscriptionRepository.findActiveByTenantId(tenantId).ifPresent(current -> {
            try {
                current.cancel();
                subscriptionRepository.save(current);
            } catch (InvalidSubscriptionTransitionException e) {
                // already canceled between the check and here — nothing to do.
            }
        });
    }
}
