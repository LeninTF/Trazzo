package trazzo.back.saasglobal.application.usecase;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.SubscriptionResult;
import trazzo.back.saasglobal.application.port.in.SubscriptionUseCase;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements SubscriptionUseCase {

    private final SubscriptionRepositoryPort subscriptionRepository;
    private final TenantRepositoryPort tenantRepository;
    private final PlanRepositoryPort planRepository;

    @Override
    public PaginatedResult<SubscriptionResult> listAll(int page, int size) {
        List<Subscription> subscriptions = subscriptionRepository.findAll(page, size);
        long total = subscriptionRepository.countAll();
        List<SubscriptionResult> results = subscriptions.stream().map(this::toResult).toList();
        return PaginatedResult.of(results, page, size, total);
    }

    private SubscriptionResult toResult(Subscription subscription) {
        String tenantName = tenantRepository.findById(subscription.getTenantId())
                .map(tenant -> tenant.getSubDomain())
                .orElse(subscription.getTenantId());
        String planName = planRepository.findById(subscription.getPlanId())
                .map(plan -> plan.getName())
                .orElse(null);
        return new SubscriptionResult(
                subscription.getId(),
                subscription.getTenantId(),
                tenantName,
                subscription.getPlanId(),
                planName,
                subscription.getDateStart(),
                subscription.getDateEnd(),
                subscription.getStatus().name(),
                subscription.getPurchasePrice(),
                subscription.getCreatedAt());
    }
}
