package trazzo.back.incidents.application.dto.command;

public record CreateEvidenceCommand(String fileName, String fileKey, String mimeType, int fileSize) {
}
