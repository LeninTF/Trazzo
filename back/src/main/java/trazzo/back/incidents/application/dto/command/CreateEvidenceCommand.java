package trazzo.back.incidents.application.dto.command;

public record CreateEvidenceCommand(String fileName, String fileUrl, String mimeType, int fileSize) {
}
