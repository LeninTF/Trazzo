package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.command.CreateDeviceCommand;
import trazzo.back.corehr.application.dto.command.PatchDeviceCommand;
import trazzo.back.corehr.application.dto.result.DeviceResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;

import java.util.Optional;

public interface DeviceUseCase {
    DeviceResult create(CreateDeviceCommand command);
    Optional<DeviceResult> findById(Long id);
    PaginatedResult<DeviceResult> findAll(Long branchId, Boolean state, int page, int size);
    DeviceResult patch(Long id, PatchDeviceCommand command);
    void deleteById(Long id);
}
