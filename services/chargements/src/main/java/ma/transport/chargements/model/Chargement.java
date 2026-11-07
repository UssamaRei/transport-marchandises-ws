package ma.transport.chargements.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Chargement {

    private String id;

    private String origine;

    private String destination;

    private String destinataireNom;

    private String destinataireTelephone;

    private String camionSouhaiteId;

    private BigDecimal poidsTotalKg;

    private StatutChargement statut;

    private int version;

    private OffsetDateTime createdAt;

    private List<LigneChargement> lignes = new ArrayList<>();

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

    public String getDestinataireNom() {
        return destinataireNom;
    }

    public void setDestinataireNom(String destinataireNom) {
        this.destinataireNom = destinataireNom;
    }

    public String getDestinataireTelephone() {
        return destinataireTelephone;
    }

    public void setDestinataireTelephone(String destinataireTelephone) {
        this.destinataireTelephone = destinataireTelephone;
    }

    public String getCamionSouhaiteId() {
        return camionSouhaiteId;
    }

    public void setCamionSouhaiteId(String camionSouhaiteId) {
        this.camionSouhaiteId = camionSouhaiteId;
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<LigneChargement> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneChargement> lignes) {
        this.lignes = lignes;
    }

}
