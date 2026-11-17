package ma.transport.flotte.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "camion", schema = "public")
public class Camion {
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "immatriculation", length = 40, nullable = false, unique = true)
    private String immatriculation;

    @Column(name = "capacite_kg", precision = 14, scale = 3, nullable = false)
    private BigDecimal capaciteKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", length = 20, nullable = false)
    private EtatCamion etat;

    @Column(name = "archive", nullable = false)
    private boolean archive;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Camion() {
    }

    public Camion(String id, String immatriculation, BigDecimal capaciteKg,
                  EtatCamion etat, boolean archive, OffsetDateTime createdAt) {
        this.id = id;
        this.immatriculation = immatriculation;
        this.capaciteKg = capaciteKg;
        this.etat = etat;
        this.archive = archive;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getImmatriculation() {
        return immatriculation;
    }

    public BigDecimal getCapaciteKg() {
        return capaciteKg;
    }

    public EtatCamion getEtat() {
        return etat;
    }

    public boolean isArchive() {
        return archive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void update(String immatriculation, BigDecimal capaciteKg) {
        this.immatriculation = immatriculation;
        this.capaciteKg = capaciteKg;
    }
}
