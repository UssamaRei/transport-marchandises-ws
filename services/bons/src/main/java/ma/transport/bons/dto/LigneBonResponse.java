package ma.transport.bons.dto;

import java.math.BigDecimal;
import ma.transport.bons.model.LigneBon;

public record LigneBonResponse(String id, String designation, int quantite, BigDecimal poidsUnitaireKg) {
    public static LigneBonResponse from(LigneBon ligne) {
        return new LigneBonResponse(ligne.getId(), ligne.getDesignation(), ligne.getQuantite(), ligne.getPoidsUnitaireKg());
    }
}
