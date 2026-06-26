package trazzo.back.incidents.domain.specification;

import trazzo.back.incidents.domain.model.IncidentType;

public class ActiveIncidentTypeSpec {

    public boolean isSatisfiedBy(IncidentType type) {
        return type != null && type.isActivo();
    }
}
