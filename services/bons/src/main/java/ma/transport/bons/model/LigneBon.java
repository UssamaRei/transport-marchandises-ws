package ma.transport.bons.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "ligne_bon")
public class LigneBon {
    @Id @Column(length = 64)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bon_id", nullable = false)
    private BonLivraison bon;
    @Column(nullable = false, length = 240)
    private String designation;
    @Column(nullable = false)
    private int quantite;
    @Column(name = "poids_unitaire_kg", nullable = false, precision = 14, scale = 3)
    private BigDecimal poidsUnitaireKg;
    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public BonLivraison getBon() { return bon; }
    public void setBon(BonLivraison v) { bon = v; }
    public String getDesignation() { return designation; }
    public void setDesignation(String v) { designation = v; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int v) { quantite = v; }
    public BigDecimal getPoidsUnitaireKg() { return poidsUnitaireKg; }
    public void setPoidsUnitaireKg(BigDecimal v) { poidsUnitaireKg = v; }
}
