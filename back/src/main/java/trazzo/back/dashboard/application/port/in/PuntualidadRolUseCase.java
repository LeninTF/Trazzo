package trazzo.back.dashboard.application.port.in;

import trazzo.back.dashboard.domain.model.PuntualidadPorRol;

import java.time.LocalDate;

public interface PuntualidadRolUseCase {
    PuntualidadPorRol getPuntualidadPorRol(LocalDate desde, LocalDate hasta, String periodo);
}
