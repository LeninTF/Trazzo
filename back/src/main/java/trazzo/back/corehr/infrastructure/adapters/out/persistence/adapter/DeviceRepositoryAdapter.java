package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.domain.model.attendance.Device;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.DeviceMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.DeviceJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceRepositoryAdapter implements DeviceRepositoryPort {

    private final DeviceJpaRepository deviceRepo;

    @Override
    @Transactional
    public Device save(Device device) {
        var entity = DeviceMapper.toEntity(device);
        var saved = deviceRepo.save(entity);
        return DeviceMapper.toDomain(saved);
    }

    @Override
    public Optional<Device> findById(Long id) {
        return deviceRepo.findById(id).map(DeviceMapper::toDomain);
    }

    @Override
    public List<Device> findAll(Long branchId, Boolean state, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return deviceRepo.findByBranchIdAndState(branchId, state, pageable)
                .stream()
                .map(DeviceMapper::toDomain)
                .toList();
    }

    @Override
    public long count(Long branchId, Boolean state) {
        return deviceRepo.countByBranchIdAndState(branchId, state);
    }

    @Override
    public boolean existsByCode(String code) {
        return deviceRepo.existsByCode(code);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        deviceRepo.deleteById(id);
    }
}
