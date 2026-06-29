package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.corehr.domain.model.attendance.Device;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.DeviceEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.DeviceJpaRepository;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceRepositoryAdapterTest {

    @Mock DeviceJpaRepository deviceRepo;
    @InjectMocks DeviceRepositoryAdapter adapter;

    @Captor ArgumentCaptor<DeviceEntity> entityCaptor;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private Device domain() {
        return Device.restore(1L, "D-001", "Device1", "192.168.1.1", 8080, "Office", 10L, true, NOW);
    }

    private DeviceEntity entity() {
        var e = new DeviceEntity();
        e.setId(1L);
        e.setCode("D-001");
        e.setName("Device1");
        e.setIp("192.168.1.1");
        e.setPuerto(8080);
        e.setUbicacion("Office");
        e.setBranchId(10L);
        e.setState(true);
        e.setCreatedAt(NOW);
        return e;
    }

    @Test
    void save_shouldPersistAndReturnDomain() {
        var domain = domain();
        var savedEntity = entity();
        when(deviceRepo.save(any(DeviceEntity.class))).thenReturn(savedEntity);

        var result = adapter.save(domain);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo("D-001");
        verify(deviceRepo).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getCode()).isEqualTo("D-001");
    }

    @Test
    void findById_shouldReturnDomainWhenFound() {
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(entity()));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getCode()).isEqualTo("D-001");
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        when(deviceRepo.findById(99L)).thenReturn(Optional.empty());

        var result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnMappedDomains() {
        var branchId = 10L;
        var state = true;
        when(deviceRepo.findByBranchIdAndState(eq(branchId), eq(state), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity())));

        var result = adapter.findAll(branchId, state, 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("D-001");
    }

    @Test
    void count_shouldDelegate() {
        when(deviceRepo.countByBranchIdAndState(10L, true)).thenReturn(5L);

        var result = adapter.count(10L, true);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void existsByCode_shouldDelegate() {
        when(deviceRepo.existsByCode("D-001")).thenReturn(true);

        assertThat(adapter.existsByCode("D-001")).isTrue();
        assertThat(adapter.existsByCode("UNKNOWN")).isFalse();
    }

    @Test
    void deleteById_shouldDelegate() {
        adapter.deleteById(1L);
        verify(deviceRepo).deleteById(1L);
    }
}
