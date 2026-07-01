package trazzo.back.corehr.domain.specification;

import java.util.Collection;
import trazzo.back.corehr.domain.model.ToleranciaType;
import trazzo.back.corehr.domain.model.schedule.Tolerancia;

public class ToleranciaUniquenessSpec {

    public boolean hasUniqueActiveType(Collection<Tolerancia> tolerancias, ToleranciaType type) {
        if (tolerancias == null || type == null) {
            return true;
        }
        return tolerancias.stream()
                .filter(t -> t.isActivo() && t.getType() == type)
                .count() <= 1;
    }
}
