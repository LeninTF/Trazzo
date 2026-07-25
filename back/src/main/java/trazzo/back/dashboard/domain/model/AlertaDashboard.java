package trazzo.back.dashboard.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record AlertaDashboard(
        List<Alerta> alertas
) {
    public record Alerta(
            String icono,
            String titulo,
            String descripcion,
            LocalDateTime fechaHora,
            String tipo
    ) {
    }
}
