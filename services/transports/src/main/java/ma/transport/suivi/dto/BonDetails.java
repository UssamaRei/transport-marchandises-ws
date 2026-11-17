package ma.transport.suivi.dto;

import java.math.BigDecimal;

public record BonDetails(
        String id,
        String statut,
        String destination,
        BigDecimal poidsTotalKg
) {
}