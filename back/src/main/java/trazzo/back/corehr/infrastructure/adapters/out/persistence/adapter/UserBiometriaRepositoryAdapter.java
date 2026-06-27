package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.port.out.UserBiometriaRepositoryPort;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.UserBiometriaMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.UserBiometriaJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBiometriaRepositoryAdapter implements UserBiometriaRepositoryPort {

    private final UserBiometriaJpaRepository userBiometriaRepo;

    @Override
    @Transactional
    public UserBiometria save(UserBiometria userBiometria) {
        var entity = UserBiometriaMapper.toEntity(userBiometria);
        var saved = userBiometriaRepo.save(entity);
        return UserBiometriaMapper.toDomain(saved);
    }

    @Override
    public Optional<UserBiometria> findById(Long id) {
        return userBiometriaRepo.findById(id).map(UserBiometriaMapper::toDomain);
    }

    @Override
    public List<UserBiometria> findAll(Long tenantUserId, Long deviceId, Boolean activo, int page, int size) {
        var pageable = PageRequest.of(page, size);
        return userBiometriaRepo.findByTenantUserIdAndDeviceIdAndActivo(tenantUserId, deviceId, activo, pageable)
                .stream()
                .map(UserBiometriaMapper::toDomain)
                .toList();
    }

    @Override
    public long count(Long tenantUserId, Long deviceId, Boolean activo) {
        return userBiometriaRepo.countByTenantUserIdAndDeviceIdAndActivo(tenantUserId, deviceId, activo);
    }

    @Override
    public Optional<UserBiometria> findByTenantUserIdAndFingerIndex(Long tenantUserId, Integer fingerIndex) {
        return userBiometriaRepo.findByTenantUserIdAndFingerIndex(tenantUserId, fingerIndex)
                .map(UserBiometriaMapper::toDomain);
    }

    @Override
    public List<UserBiometria> findByTenantUserId(Long tenantUserId) {
        return userBiometriaRepo.findByTenantUserId(tenantUserId)
                .stream()
                .map(UserBiometriaMapper::toDomain)
                .toList();
    }
}
