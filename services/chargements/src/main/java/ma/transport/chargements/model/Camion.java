package ma.transport.chargements.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Camion {

    private String id;

    private BigDecimal capaciteKg;

    private String etat;

    private Boolean archive;

    private Boolean disponible;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getCapaciteKg() {
        return capaciteKg;
    }

    public void setCapaciteKg(BigDecimal capaciteKg) {
        this.capaciteKg = capaciteKg;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public Boolean getArchive() {
        return archive;
    }

    public void setArchive(Boolean archive) {
        this.archive = archive;
    }

    public Boolean getDisponible() {
        return disponible;
    }

    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }

}
