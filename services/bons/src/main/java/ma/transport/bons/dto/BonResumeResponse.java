package ma.transport.bons.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ma.transport.bons.model.BonLivraison;

public record BonResumeResponse(String id, String numero, String chargementId, String camionSouhaiteId,
                                String destination, BigDecimal poidsTotalKg, String statut, Instant createdAt) {
    public static BonResumeResponse from(BonLivraison bon) {
        return new BonResumeResponse(bon.getId(), bon.getNumero(), bon.getChargementId(), bon.getCamionSouhaiteId(),
                bon.getDestination(), bon.getPoidsTotalKg(), bon.getStatut().name(),
                bon.getCreatedAt() == null ? null : bon.getCreatedAt().toInstant());
    }
}
