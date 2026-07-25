package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.attendance.Device;

import java.util.List;
import java.util.Optional;

public interface DeviceRepositoryPort {
    Device save(Device device);
    Optional<Device> findById(Long id);
    List<Device> findAll(Long branchId, Boolean state, int page, int size);
    long count(Long branchId, Boolean state);
    boolean existsByCode(String code);
    Optional<Device> findByCode(String code);
    void deleteById(Long id);
}
