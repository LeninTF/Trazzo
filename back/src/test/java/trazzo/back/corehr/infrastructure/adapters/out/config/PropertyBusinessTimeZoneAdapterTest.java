package trazzo.back.corehr.infrastructure.adapters.out.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyBusinessTimeZoneAdapterTest {

    @Test
    void usesTheConfiguredZone() {
        assertThat(new PropertyBusinessTimeZoneAdapter("America/Bogota").zone()).isEqualTo(ZoneId.of("America/Bogota"));
    }

    // "APP_TIMEZONE=" en el .env deja la propiedad vacía, no ausente: cuenta como no configurada.
    @Test
    void aBlankZoneMeansLima() {
        assertThat(new PropertyBusinessTimeZoneAdapter(" ").zone()).isEqualTo(ZoneId.of("America/Lima"));
    }

    @Test
    void anInvalidZoneFailsAtStartupNamingTheProperty() {
        assertThatThrownBy(() -> new PropertyBusinessTimeZoneAdapter("Marte/Olympus"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trazzo.attendance.time-zone");
    }
}
