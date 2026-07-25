package trazzo.back.dashboard.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.dashboard.domain.model.AlertaDashboard;

import java.time.LocalDateTime;
import java.util.List;

public record AlertasResponse(
        List<AlertaItem> alertas
) {
    public static AlertasResponse from(AlertaDashboard model) {
        var items = model.alertas().stream()
                .map(a -> new AlertaItem(
                        a.icono(), a.titulo(), a.descripcion(), a.fechaHora(), a.tipo()))
                .toList();
        return new AlertasResponse(items);
    }

    public record AlertaItem(
            String icono,
            String titulo,
            String descripcion,
            @JsonProperty("fecha_hora") LocalDateTime fechaHora,
            String tipo
    ) {
    }
}
