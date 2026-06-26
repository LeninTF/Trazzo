package trazzo.back.saasglobal.application.dto.command;

import java.time.LocalDate;

public record ActivateSubscriptionCommand(
        String subscriptionId,
        LocalDate dateEnd
) {
}
