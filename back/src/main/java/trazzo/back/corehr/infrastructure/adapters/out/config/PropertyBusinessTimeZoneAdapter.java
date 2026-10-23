package trazzo.back.corehr.infrastructure.adapters.out.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import trazzo.back.corehr.application.port.out.BusinessTimeZonePort;

import java.time.DateTimeException;
import java.time.ZoneId;

@Component
public class PropertyBusinessTimeZoneAdapter implements BusinessTimeZonePort {

    static final String PROPERTY = "trazzo.attendance.time-zone";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Lima");

    private final ZoneId zone;

    // Vacía cuenta como no configurada ("APP_TIMEZONE=" en el .env); una zona inválida impide arrancar.
    public PropertyBusinessTimeZoneAdapter(@Value("${" + PROPERTY + ":}") String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            this.zone = DEFAULT_ZONE;
            return;
        }
        try {
            this.zone = ZoneId.of(zoneId.trim());
        } catch (DateTimeException e) {
            throw new IllegalStateException(PROPERTY + " no es una zona horaria válida: " + zoneId, e);
        }
    }

    @Override
    public ZoneId zone() {
        return zone;
    }
}
