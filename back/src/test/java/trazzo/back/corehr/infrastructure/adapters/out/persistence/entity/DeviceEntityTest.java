package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceEntityTest {

    @Test
    void noArgsConstructor() {
        var entity = new DeviceEntity();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCode()).isNull();
    }

    @Test
    void allArgsConstructor() {
        var now = LocalDateTime.now();
        var entity = new DeviceEntity(1L, "D-001", "Device1", "192.168.1.1", 8080, "Office",
                10L, true, now);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getCode()).isEqualTo("D-001");
        assertThat(entity.getName()).isEqualTo("Device1");
        assertThat(entity.getIp()).isEqualTo("192.168.1.1");
        assertThat(entity.getPuerto()).isEqualTo(8080);
        assertThat(entity.getUbicacion()).isEqualTo("Office");
        assertThat(entity.getBranchId()).isEqualTo(10L);
        assertThat(entity.isState()).isTrue();
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void settersAndGetters() {
        var entity = new DeviceEntity();

        entity.setId(2L);
        entity.setCode("D-002");
        entity.setName("Device2");
        entity.setIp("10.0.0.1");
        entity.setPuerto(9090);
        entity.setUbicacion("Warehouse");
        entity.setBranchId(20L);
        entity.setState(false);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getCode()).isEqualTo("D-002");
        assertThat(entity.getName()).isEqualTo("Device2");
        assertThat(entity.getIp()).isEqualTo("10.0.0.1");
        assertThat(entity.getPuerto()).isEqualTo(9090);
        assertThat(entity.getUbicacion()).isEqualTo("Warehouse");
        assertThat(entity.getBranchId()).isEqualTo(20L);
        assertThat(entity.isState()).isFalse();
    }

    @Test
    void onCreateSetsCreatedAt() {
        var entity = new DeviceEntity();
        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void onCreateAlwaysOverwritesCreatedAt() {
        var entity = new DeviceEntity();
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
        assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
