package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.command.CreateDeviceCommand;
import trazzo.back.corehr.application.dto.command.PatchDeviceCommand;
import trazzo.back.corehr.application.dto.result.DeviceResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.port.in.DeviceUseCase;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.domain.model.attendance.Device;

import java.util.Optional;

@RequiredArgsConstructor
public class DeviceService implements DeviceUseCase {

    private final DeviceRepositoryPort deviceRepository;

    @Override
    public DeviceResult create(CreateDeviceCommand command) {
        if (deviceRepository.existsByCode(command.code())) {
            throw new IllegalArgumentException("El code (serial) ya está registrado en este tenant");
        }
        var device = Device.create(command.code(), command.name(), command.ip(),
                command.puerto(), command.ubicacion(), command.branchId());
        var saved = deviceRepository.save(device);
        return toResult(saved);
    }

    @Override
    public Optional<DeviceResult> findById(Long id) {
        return deviceRepository.findById(id).map(this::toResult);
    }

    @Override
    public PaginatedResult<DeviceResult> findAll(Long branchId, Boolean state, int page, int size) {
        var devices = deviceRepository.findAll(branchId, state, page, size);
        var total = deviceRepository.count(branchId, state);
        var results = devices.stream().map(this::toResult).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public DeviceResult patch(Long id, PatchDeviceCommand command) {
        var device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado: " + id));
        if (command.name() != null || command.ip() != null || command.puerto() != null || command.ubicacion() != null) {
            device.updateLocation(
                    command.ip() != null ? command.ip() : device.getIp(),
                    command.puerto() != null ? command.puerto() : device.getPuerto(),
                    command.ubicacion() != null ? command.ubicacion() : device.getUbicacion()
            );
        }
        if (command.state() != null) {
            if (command.state()) {
                device.activate();
            } else {
                device.deactivate();
            }
        }
        var saved = deviceRepository.save(device);
        return toResult(saved);
    }

    @Override
    public void deleteById(Long id) {
        if (!deviceRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Dispositivo no encontrado: " + id);
        }
        deviceRepository.deleteById(id);
    }

    private DeviceResult toResult(Device device) {
        return new DeviceResult(
                device.getId(),
                device.getCode(),
                device.getName(),
                device.getBranchId(),
                null,
                device.getIp(),
                device.getPuerto(),
                device.getUbicacion(),
                device.isState(),
                device.getCreatedAt()
        );
    }
}
