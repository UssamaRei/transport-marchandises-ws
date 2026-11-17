package ma.transport.chargements.dto;

import java.math.BigDecimal;
import ma.transport.chargements.model.StatutChargement;

public class ChargementResume {

    private String id;

    private String origine;

    private String destination;

    private BigDecimal poidsTotalKg;

    private StatutChargement statut;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public BigDecimal getPoidsTotalKg() {
        return poidsTotalKg;
    }

    public void setPoidsTotalKg(BigDecimal poidsTotalKg) {
        this.poidsTotalKg = poidsTotalKg;
    }

    public StatutChargement getStatut() {
        return statut;
    }

    public void setStatut(StatutChargement statut) {
        this.statut = statut;
    }

}
