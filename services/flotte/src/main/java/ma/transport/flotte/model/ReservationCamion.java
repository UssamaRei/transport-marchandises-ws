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
@Table(name = "reservation_camion", schema = "public")
public class ReservationCamion {
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "camion_id", length = 64, nullable = false)
    private String camionId;

    @Column(name = "transport_id", length = 64, nullable = false, unique = true)
    private String transportId;

    @Column(name = "poids_kg", precision = 14, scale = 3, nullable = false)
    private BigDecimal poidsKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20, nullable = false)
    private StatutReservation statut;

    @Column(name = "motif_liberation", length = 20)
    private String motifLiberation;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    protected ReservationCamion() {
    }

    public ReservationCamion(String id, String camionId, String transportId, BigDecimal poidsKg,
                            StatutReservation statut, String motifLiberation,
                            OffsetDateTime createdAt, OffsetDateTime releasedAt) {
        this.id = id;
        this.camionId = camionId;
        this.transportId = transportId;
        this.poidsKg = poidsKg;
        this.statut = statut;
        this.motifLiberation = motifLiberation;
        this.createdAt = createdAt;
        this.releasedAt = releasedAt;
    }

    public String getId() {
        return id;
    }

    public String getCamionId() {
        return camionId;
    }

    public String getTransportId() {
        return transportId;
    }

    public BigDecimal getPoidsKg() {
        return poidsKg;
    }

    public StatutReservation getStatut() {
        return statut;
    }

    public String getMotifLiberation() {
        return motifLiberation;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getReleasedAt() {
        return releasedAt;
    }

    public void release(String motif, OffsetDateTime releasedAt) {
        this.statut = StatutReservation.LIBEREE;
        this.motifLiberation = motif;
        this.releasedAt = releasedAt;
    }
}
