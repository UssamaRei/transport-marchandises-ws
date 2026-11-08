package ma.transport.chargements.model;

import java.math.BigDecimal;

public class LigneChargement {

    private String id;

    private String designation;

    private int quantite;

    private BigDecimal poidsUnitaireKg;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPoidsUnitaireKg() {
        return poidsUnitaireKg;
    }

    public void setPoidsUnitaireKg(BigDecimal poidsUnitaireKg) {
        this.poidsUnitaireKg = poidsUnitaireKg;
    }

}
