package ma.transport.suivi.dto;

import java.time.OffsetDateTime;

public record PlanificationRequest(
        String bonId,
        String camionId,
        OffsetDateTime departPrevu,
        String operationKey
) {
    public PlanificationRequest {
        departPrevu = Dates.normaliser(departPrevu);
    }
}
