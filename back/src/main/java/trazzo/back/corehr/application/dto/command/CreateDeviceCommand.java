package trazzo.back.corehr.application.dto.command;

public record CreateDeviceCommand(String code, String name, Long branchId,
                                  String ip, Integer puerto, String ubicacion) {
}
