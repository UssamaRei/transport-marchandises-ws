package ma.transport.flotte.dto;

import ma.transport.flotte.model.EtatCamion;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CamionResponse(
        String id,
        String immatriculation,
        BigDecimal capaciteKg,
        EtatCamion etat,
        boolean archive,
        boolean disponible,
        OffsetDateTime createdAt) {
}
