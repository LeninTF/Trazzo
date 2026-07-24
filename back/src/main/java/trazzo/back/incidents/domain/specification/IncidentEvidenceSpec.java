package trazzo.back.incidents.domain.specification;

import java.util.Collection;
import java.util.Set;
import trazzo.back.incidents.domain.model.IncidentEvidence;

public class IncidentEvidenceSpec {

    public static final int MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024;

    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "video/mp4",
            "video/quicktime"
    );

    public boolean allowsSingleActiveEvidence(Collection<IncidentEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return true;
        }
        return evidences.stream().filter(evidence -> !evidence.isDeleted()).count() <= 1;
    }

    public boolean isValidFileSize(int fileSize) {
        return fileSize > 0 && fileSize <= MAX_FILE_SIZE_BYTES;
    }

    public boolean isAllowedMimeType(String mimeType) {
        return mimeType != null && ALLOWED_MIME_TYPES.contains(mimeType.trim().toLowerCase());
    }
}
