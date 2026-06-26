package trazzo.back.incidents.domain.specification;

import java.util.Collection;
import trazzo.back.incidents.domain.model.IncidentEvidence;

public class IncidentEvidenceSpec {

    public static final int MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024;

    public boolean allowsSingleActiveEvidence(Collection<IncidentEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return true;
        }
        return evidences.stream().filter(evidence -> !evidence.isDeleted()).count() <= 1;
    }

    public boolean isValidFileSize(int fileSize) {
        return fileSize > 0 && fileSize <= MAX_FILE_SIZE_BYTES;
    }
}
