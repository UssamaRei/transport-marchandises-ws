package ma.transport.suivi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "evenement_suivi", schema = "public")
public class EvenementSuivi {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "transport_id", length = 64, nullable = false)
    private String transportId;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private TypeEvenement type;

    @Column(length = 1000, nullable = false)
    private String description;

    @Column(name = "date_evenement", nullable = false)
    private OffsetDateTime dateEvenement;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 7)
    private BigDecimal longitude;

    protected EvenementSuivi() {
        // Constructeur requis par JPA
    }

    public EvenementSuivi(
            String transportId,
            TypeEvenement type,
            String description,
            OffsetDateTime dateEvenement,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if (transportId == null || transportId.isBlank()
                || transportId.length() > 64) {
            throw new IllegalArgumentException("Transport obligatoire");
        }

        if (description == null || description.isBlank()
                || description.length() > 1000) {
            throw new IllegalArgumentException(
                    "Description obligatoire, maximum 1000 caractères"
            );
        }

        if (latitude != null
                && (latitude.compareTo(new BigDecimal("-90")) < 0
                || latitude.compareTo(new BigDecimal("90")) > 0)) {
            throw new IllegalArgumentException("Latitude invalide");
        }

        if (longitude != null
                && (longitude.compareTo(new BigDecimal("-180")) < 0
                || longitude.compareTo(new BigDecimal("180")) > 0)) {
            throw new IllegalArgumentException("Longitude invalide");
        }

        this.id = UUID.randomUUID().toString();
        this.transportId = transportId;
        this.type = Objects.requireNonNull(type, "Type obligatoire");
        this.description = description;
        this.dateEvenement = Objects.requireNonNull(
                dateEvenement, "Date obligatoire"
        );
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getTransportId() {
        return transportId;
    }

    public TypeEvenement getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getDateEvenement() {
        return dateEvenement;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }
}
