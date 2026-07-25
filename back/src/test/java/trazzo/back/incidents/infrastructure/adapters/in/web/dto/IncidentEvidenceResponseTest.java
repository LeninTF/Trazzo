package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.application.dto.result.IncidentEvidenceResult;

import java.time.LocalDateTime;

class IncidentEvidenceResponseTest {

    @Test
    void fromResultMapsAllFields() {
        var now = LocalDateTime.now();
        var result = new IncidentEvidenceResult(1, 1, "doc.pdf",
                "file-key", "/api/v1/incidentes/1/evidencias/1/descarga", "pdf", 100, now, now);
        var response = IncidentEvidenceResponse.from(result);

        assertEquals("1", response.id());
        assertEquals("1", response.incidenciaId());
        assertEquals("doc.pdf", response.fileName());
        assertEquals("/api/v1/incidentes/1/evidencias/1/descarga", response.downloadUrl());
        assertEquals("file-key", response.fileKey());
        assertEquals("pdf", response.mimeType());
        assertEquals(100, response.fileSize());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }
}
