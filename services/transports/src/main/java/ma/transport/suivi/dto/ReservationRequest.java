package ma.transport.suivi.dto;

import java.math.BigDecimal;

public record ReservationRequest(
        String camionId,
        String transportId,
        BigDecimal poidsKg
) {
}
