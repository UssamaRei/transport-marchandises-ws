package ma.transport.flotte.dto;

import ma.transport.flotte.model.StatutReservation;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ReservationResponse(
        String id,
        String camionId,
        String transportId,
        BigDecimal poidsKg,
        StatutReservation statut,
        String motifLiberation,
        OffsetDateTime createdAt,
        OffsetDateTime releasedAt) {
}
