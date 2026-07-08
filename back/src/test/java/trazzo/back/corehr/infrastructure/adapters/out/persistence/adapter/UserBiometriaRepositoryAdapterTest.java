package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.UserBiometriaEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.UserBiometriaJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBiometriaRepositoryAdapterTest {

    private static final String TEMPLATE = "YmFzZTY0dGVtcGxhdGU=";
    private static final String AES_KEY = "YmFzZTY0YWVzS2V5";

    @Mock
    private UserBiometriaJpaRepository userBiometriaRepo;

    @InjectMocks
    private UserBiometriaRepositoryAdapter adapter;

    private final LocalDateTime now = LocalDateTime.now();

    private UserBiometriaEntity createEntity(Long id) {
        var e = new UserBiometriaEntity();
        e.setId(id);
        e.setTenantUserId(100L);
        e.setDeviceId(200L);
        e.setDeviceCode("DVC-001");
        e.setFingerIndex(1);
        e.setEncryptedTemplateBase64(TEMPLATE);
        e.setEncryptedAesKeyBase64(AES_KEY);
        e.setCapturadoEn(now);
        e.setActivo(true);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }

    @Test
    void save_shouldPersistAndReturnDomain() {
        var domain = UserBiometria.restore(null, 100L, 200L, "DVC-001", 1, TEMPLATE, AES_KEY, null, null, now, true, now, now);
        var entity = createEntity(1L);
        when(userBiometriaRepo.save(any())).thenReturn(entity);

        var result = adapter.save(domain);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTenantUserId()).isEqualTo(100L);
        verify(userBiometriaRepo).save(any());
    }

    @Test
    void findById_whenExists_shouldReturnDomain() {
        var entity = createEntity(1L);
        when(userBiometriaRepo.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        when(userBiometriaRepo.findById(99L)).thenReturn(Optional.empty());

        var result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnMappedList() {
        var entity = createEntity(1L);
        when(userBiometriaRepo.findByTenantUserIdAndDeviceIdAndActivo(100L, 200L, true, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(100L, 200L, true, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceId()).isEqualTo(200L);
    }

    @Test
    void count_shouldReturnCount() {
        when(userBiometriaRepo.countByTenantUserIdAndDeviceIdAndActivo(100L, 200L, true)).thenReturn(3L);

        var result = adapter.count(100L, 200L, true);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void findByTenantUserIdAndFingerIndex_whenExists_shouldReturnDomain() {
        var entity = createEntity(1L);
        when(userBiometriaRepo.findByTenantUserIdAndFingerIndex(100L, 1)).thenReturn(Optional.of(entity));

        var result = adapter.findByTenantUserIdAndFingerIndex(100L, 1);

        assertThat(result).isPresent();
        assertThat(result.get().getFingerIndex()).isEqualTo(1);
    }

    @Test
    void findByTenantUserIdAndFingerIndex_whenNotExists_shouldReturnEmpty() {
        when(userBiometriaRepo.findByTenantUserIdAndFingerIndex(99L, 1)).thenReturn(Optional.empty());

        var result = adapter.findByTenantUserIdAndFingerIndex(99L, 1);

        assertThat(result).isEmpty();
    }

    @Test
    void findByTenantUserId_shouldReturnMappedList() {
        var entity = createEntity(1L);
        when(userBiometriaRepo.findByTenantUserId(100L)).thenReturn(List.of(entity));

        var result = adapter.findByTenantUserId(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantUserId()).isEqualTo(100L);
    }

    @Test
    void findByTenantUserId_whenEmpty_shouldReturnEmptyList() {
        when(userBiometriaRepo.findByTenantUserId(99L)).thenReturn(List.of());

        var result = adapter.findByTenantUserId(99L);

        assertThat(result).isEmpty();
    }
}
