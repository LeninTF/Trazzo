package trazzo.back.incidents.application.dto.command;

public record PatchIncidentTypeCommand(String nombre, String descripcion, Boolean activo) {
}
