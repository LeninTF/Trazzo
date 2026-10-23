package trazzo.back.corehr.application.port.out;

import java.time.ZoneId;

/** Zona en la que se leen los horarios del negocio: un ingreso a las 08:00 son las 08:00 locales. */
public interface BusinessTimeZonePort {
    ZoneId zone();
}
