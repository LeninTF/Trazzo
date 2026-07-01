package trazzo.back.corehr.application.dto.command;

public record PatchToleranciaCommand(String name, Integer minutes, String description, Boolean activo) {
}
