package ma.transport.bons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "bon_livraison", uniqueConstraints = {
        @UniqueConstraint(name = "bon_livraison_numero_key", columnNames = "numero"),
        @UniqueConstraint(name = "bon_livraison_chargement_id_key", columnNames = "chargement_id"),
        @UniqueConstraint(name = "bon_livraison_transport_preuve_id_key", columnNames = "transport_preuve_id")
})
public class BonLivraison {
    @Id @Column(length = 64)
    private String id;
    @Column(nullable = false, length = 64)
    private String numero;
    @Column(name = "chargement_id", nullable = false, length = 64)
    private String chargementId;
    @Column(name = "camion_souhaite_id", nullable = false, length = 64)
    private String camionSouhaiteId;
    @Column(nullable = false, length = 500)
    private String origine;
    @Column(nullable = false, length = 500)
    private String destination;
    @Column(name = "destinataire_nom", nullable = false, length = 160)
    private String destinataireNom;
    @Column(name = "destinataire_telephone", length = 40)
    private String destinataireTelephone;
    @Column(name = "poids_total_kg", nullable = false, precision = 14, scale = 3)
    private BigDecimal poidsTotalKg;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private StatutBon statut = StatutBon.EMIS;
    @Column(name = "transport_preuve_id", length = 64)
    private String transportPreuveId;
    @Column(length = 160)
    private String recepteur;
    @Column(name = "livre_le")
    private OffsetDateTime livreLe;
    @Column(length = 1000)
    private String commentaire;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Generated(event = EventType.INSERT)
    private OffsetDateTime createdAt;
    @OneToMany(mappedBy = "bon", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LigneBon> lignes = new ArrayList<>();

    public void addLigne(LigneBon ligne) { lignes.add(ligne); ligne.setBon(this); }
    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getNumero() { return numero; } public void setNumero(String v) { numero = v; }
    public String getChargementId() { return chargementId; } public void setChargementId(String v) { chargementId = v; }
    public String getCamionSouhaiteId() { return camionSouhaiteId; } public void setCamionSouhaiteId(String v) { camionSouhaiteId = v; }
    public String getOrigine() { return origine; } public void setOrigine(String v) { origine = v; }
    public String getDestination() { return destination; } public void setDestination(String v) { destination = v; }
    public String getDestinataireNom() { return destinataireNom; } public void setDestinataireNom(String v) { destinataireNom = v; }
    public String getDestinataireTelephone() { return destinataireTelephone; } public void setDestinataireTelephone(String v) { destinataireTelephone = v; }
    public BigDecimal getPoidsTotalKg() { return poidsTotalKg; } public void setPoidsTotalKg(BigDecimal v) { poidsTotalKg = v; }
    public StatutBon getStatut() { return statut; } public void setStatut(StatutBon v) { statut = v; }
    public String getTransportPreuveId() { return transportPreuveId; } public void setTransportPreuveId(String v) { transportPreuveId = v; }
    public String getRecepteur() { return recepteur; } public void setRecepteur(String v) { recepteur = v; }
    public OffsetDateTime getLivreLe() { return livreLe; } public void setLivreLe(OffsetDateTime v) { livreLe = v; }
    public String getCommentaire() { return commentaire; } public void setCommentaire(String v) { commentaire = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<LigneBon> getLignes() { return lignes; }
}
