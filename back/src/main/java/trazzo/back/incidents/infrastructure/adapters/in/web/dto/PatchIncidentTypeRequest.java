package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PatchIncidentTypeRequest(
        String nombre,
        String descripcion,
        Boolean activo
) {
    @JsonProperty("activo")
    public Boolean activo() {
        return activo;
    }
}
