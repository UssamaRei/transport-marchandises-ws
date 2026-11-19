package ma.transport.suivi.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ma.transport.suivi.model.TypeEvenement;

public record EvenementRequest(
        TypeEvenement type,
        String description,
        OffsetDateTime dateEvenement,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public EvenementRequest {
        dateEvenement = Dates.normaliser(dateEvenement);
    }
}
