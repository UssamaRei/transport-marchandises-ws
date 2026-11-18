package ma.transport.bons.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import ma.transport.bons.model.BonLivraison;

public record BonResponse(
        String id, String numero, String chargementId, String camionSouhaiteId,
        String origine, String destination, String destinataireNom, String destinataireTelephone,
        BigDecimal poidsTotalKg, String statut, String transportId, String recepteur,
        LocalDate dateReception, String commentaire, Instant createdAt, List<LigneBonResponse> lignes
) {
    public static BonResponse from(BonLivraison bon) {
        LocalDate date = bon.getLivreLe() == null ? null : bon.getLivreLe().toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        return new BonResponse(bon.getId(), bon.getNumero(), bon.getChargementId(), bon.getCamionSouhaiteId(),
                bon.getOrigine(), bon.getDestination(), bon.getDestinataireNom(), bon.getDestinataireTelephone(),
                bon.getPoidsTotalKg(), bon.getStatut().name(), bon.getTransportPreuveId(), bon.getRecepteur(), date,
                bon.getCommentaire(), bon.getCreatedAt() == null ? null : bon.getCreatedAt().toInstant(),
                bon.getLignes().stream().map(LigneBonResponse::from).toList());
    }
}
