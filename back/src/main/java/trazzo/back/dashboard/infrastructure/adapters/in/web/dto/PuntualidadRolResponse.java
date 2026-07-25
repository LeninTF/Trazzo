package trazzo.back.dashboard.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.dashboard.domain.model.PuntualidadPorRol;

import java.util.List;

public record PuntualidadRolResponse(
        String periodo,
        String desde,
        String hasta,
        List<RolPuntualidadItem> roles
) {
    public static PuntualidadRolResponse from(PuntualidadPorRol model) {
        var items = model.roles().stream()
                .map(r -> new RolPuntualidadItem(r.nombre(), r.porcentaje()))
                .toList();
        return new PuntualidadRolResponse(model.periodo(), model.desde(), model.hasta(), items);
    }

    public record RolPuntualidadItem(
            String nombre,
            double porcentaje
    ) {
    }
}
