package ma.transport.bons.dto;

import java.math.BigDecimal;
import java.util.List;

public record ChargementDetails(
        String id,
        String origine,
        String destination,
        String destinataireNom,
        String destinataireTelephone,
        String camionSouhaiteId,
        BigDecimal poidsTotalKg,
        String statut,
        List<LigneChargementDetails> lignes
) {
    public record LigneChargementDetails(String id, String designation, int quantite, BigDecimal poidsUnitaireKg) { }
}
