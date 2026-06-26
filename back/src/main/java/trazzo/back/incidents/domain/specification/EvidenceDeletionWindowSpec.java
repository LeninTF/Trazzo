package trazzo.back.incidents.domain.specification;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import trazzo.back.incidents.domain.model.IncidentEvidence;

public class EvidenceDeletionWindowSpec {

    public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);

    private final Duration window;

    public EvidenceDeletionWindowSpec() {
        this(DEFAULT_WINDOW);
    }

    public EvidenceDeletionWindowSpec(Duration window) {
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("window must be greater than zero");
        }
        this.window = window;
    }

    public boolean isSatisfiedBy(IncidentEvidence evidence, Clock clock) {
        if (evidence == null || evidence.getUploadedAt() == null) {
            return false;
        }
        LocalDateTime deadline = evidence.getUploadedAt().plus(window);
        return !LocalDateTime.now(clock).isAfter(deadline);
    }
}
