package trazzo.back.dashboard.domain.model;

import java.util.List;

public record PuntualidadPorRol(
        String periodo,
        String desde,
        String hasta,
        List<RolPuntualidad> roles
) {
    public record RolPuntualidad(String nombre, double porcentaje) {
    }
}
