package ma.transport.suivi.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ma.transport.suivi.model.EvenementSuivi;
import ma.transport.suivi.model.TypeEvenement;

public record EvenementResponse(
        String id,
        String transportId,
        TypeEvenement type,
        String description,
        OffsetDateTime dateEvenement,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public static EvenementResponse from(EvenementSuivi evenement) {
        return new EvenementResponse(
                evenement.getId(),
                evenement.getTransportId(),
                evenement.getType(),
                evenement.getDescription(),
                evenement.getDateEvenement(),
                evenement.getLatitude(),
                evenement.getLongitude()
        );
    }
}
