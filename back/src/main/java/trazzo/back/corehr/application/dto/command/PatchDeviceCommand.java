package trazzo.back.corehr.application.dto.command;

public record PatchDeviceCommand(String name, Long branchId, String ip,
                                 Integer puerto, String ubicacion, Boolean state) {
}
