package trazzo.back.incidents.infrastructure.adapters.out.persistence.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    public static Specification<IncidentEntity> byFilters(
            Integer tenantUserId,
            IncidentState state,
            Integer incidentTypeId,
            LocalDateTime desde,
            LocalDateTime hasta,
            String search
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenantUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("tenantUserId"), tenantUserId));
            }
            if (state != null) {
                predicates.add(criteriaBuilder.equal(root.get("state"), state));
            }
            if (incidentTypeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("incidentTypeId"), incidentTypeId));
            }
            if (desde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), desde));
            }
            if (hasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), hasta));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("comment")),
                        "%" + search.toLowerCase(Locale.ROOT) + "%"
                ));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
