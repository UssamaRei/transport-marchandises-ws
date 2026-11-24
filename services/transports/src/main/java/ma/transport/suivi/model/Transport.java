package ma.transport.suivi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** Entité JPA et règles métier initiales, sans appels réseau ni accès à la base. */
@Entity
@Table(name = "transport", schema = "public")
public class Transport {
    @Id @Column(length = 64, nullable = false)
    private String id;
    @Column(name = "bon_id", length = 64, nullable = false)
    private String bonId;
    @Column(name = "camion_id", length = 64, nullable = false)
    private String camionId;
    @Column(name = "reservation_id", length = 64, unique = true)
    private String reservationId;
    @Column(name = "poids_kg", precision = 14, scale = 3, nullable = false)
    private BigDecimal poidsKg;
    @Column(length = 500, nullable = false)
    private String destination;
    @Column(name = "depart_prevu", nullable = false)
    private OffsetDateTime departPrevu;
    @Column(name = "depart_reel")
    private OffsetDateTime departReel;
    @Column(name = "arrivee_reelle")
    private OffsetDateTime arriveeReelle;
    @Enumerated(EnumType.STRING) @Column(length = 32, nullable = false)
    private StatutTransport statut;
    @Enumerated(EnumType.STRING) @Column(name = "sync_etat", length = 20, nullable = false)
    private SyncEtat syncEtat;
    @Column(name = "operation_key", length = 160, nullable = false, unique = true)
    private String operationKey;
    @Column(name = "preuve_confirmee", nullable = false)
    private boolean preuveConfirmee;
    @Column(name = "liberation_confirmee", nullable = false)
    private boolean liberationConfirmee;
    @Version @Column(nullable = false)
    private int version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "preuve_receptionnaire", length = 160)
    private String preuveReceptionnaire;

    @Column(name = "preuve_date_reception")
    private java.time.LocalDate preuveDateReception;

    @Column(name = "preuve_commentaire", length = 1000)
    private String preuveCommentaire;

    protected Transport() { } // requis par JPA

    public Transport(String bonId, String camionId, BigDecimal poidsKg, String destination,
                     OffsetDateTime departPrevu, String operationKey, OffsetDateTime createdAt) {
        this.id = UUID.randomUUID().toString();
        this.bonId = texteRequis(bonId, 64, "bonId");
        this.camionId = texteRequis(camionId, 64, "camionId");
        this.destination = texteRequis(destination, 500, "destination");
        this.operationKey = texteRequis(operationKey, 160, "operationKey");
        if (poidsKg == null || poidsKg.signum() <= 0 || poidsKg.scale() > 3 || poidsKg.precision() - poidsKg.scale() > 11) {
            throw new IllegalArgumentException("poidsKg doit être positif et tenir dans numeric(14,3)");
        }
        this.poidsKg = poidsKg;
        this.departPrevu = Objects.requireNonNull(departPrevu, "departPrevu");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.statut = StatutTransport.PREPARATION;
        this.syncEtat = SyncEtat.A_REPRENDRE;
    }

    /** À appeler uniquement après confirmation effective par le service Camions. */
    public void confirmerReservation(String reservationId) {
        exigerStatut(StatutTransport.PREPARATION);
        this.reservationId = texteRequis(reservationId, 64, "reservationId");
        this.statut = StatutTransport.PLANIFIE;
        this.syncEtat = SyncEtat.OK;
    }

    public void demarrer(OffsetDateTime dateDepart) {
        exigerStatut(StatutTransport.PLANIFIE);
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalStateException("Réservation confirmée obligatoire avant départ");
        }
        this.departReel = Objects.requireNonNull(dateDepart, "dateDepart");
        this.statut = StatutTransport.EN_COURS;
    }

    public void enregistrerArrivee(OffsetDateTime dateArrivee) {
        exigerStatut(StatutTransport.EN_COURS);
        Objects.requireNonNull(dateArrivee, "dateArrivee");
        if (departReel == null || dateArrivee.isBefore(departReel)) {
            throw new IllegalArgumentException("Arrivée antérieure au départ");
        }
        this.arriveeReelle = dateArrivee;
        this.statut = StatutTransport.CLOTURE_EN_ATTENTE;
        this.syncEtat = SyncEtat.A_REPRENDRE;
    }


    public void confirmerPreuve() {
        exigerStatut(StatutTransport.CLOTURE_EN_ATTENTE);
        this.preuveConfirmee = true;
    }
    public void confirmerLiberation() {
        exigerStatut(StatutTransport.CLOTURE_EN_ATTENTE);
        this.liberationConfirmee = true;
    }
    public void finaliserLivraison() {
        exigerStatut(StatutTransport.CLOTURE_EN_ATTENTE);
        if (!preuveConfirmee || !liberationConfirmee) {
            throw new IllegalStateException("Preuve et libération confirmées obligatoires");
        }
        this.statut = StatutTransport.LIVRE;
        this.syncEtat = SyncEtat.OK;
    }
    private void exigerStatut(StatutTransport attendu) {
        if (statut != attendu) {
            throw new IllegalStateException("Statut attendu : " + attendu + ", obtenu : " + statut);
        }
    }
    private static String texteRequis(String valeur, int max, String nom) {
        if (valeur == null || valeur.isBlank() || valeur.length() > max) {
            throw new IllegalArgumentException(nom + " obligatoire, maximum " + max + " caractères");
        }
        return valeur;
    }
    public String getId() { return id; }
    public String getReservationId() { return reservationId; }
    public StatutTransport getStatut() { return statut; }
    public SyncEtat getSyncEtat() { return syncEtat; }
    public OffsetDateTime getDepartReel() { return departReel; }
    public OffsetDateTime getArriveeReelle() { return arriveeReelle; }
    public boolean isPreuveConfirmee() { return preuveConfirmee; }
    public boolean isLiberationConfirmee() { return liberationConfirmee; }


    /**
     * Commence l'annulation d'un transport planifié.
     * La libération du camion doit encore être confirmée par Camions.
     */
    public void demanderAnnulation() {
        if (statut == StatutTransport.ANNULATION_EN_ATTENTE || statut == StatutTransport.ANNULE) {
            return;
        }

        exigerStatut(StatutTransport.PLANIFIE);

        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalStateException(
                    "Réservation obligatoire pour annuler ce transport"
            );
        }

        this.statut = StatutTransport.ANNULATION_EN_ATTENTE;
        this.syncEtat = SyncEtat.A_REPRENDRE;
    }

    /**
     * À appeler uniquement après confirmation de libération par Camions.
     */
    public void confirmerLiberationEtFinaliserAnnulation() {
        if (statut == StatutTransport.ANNULE) {
            return;
        }

        exigerStatut(StatutTransport.ANNULATION_EN_ATTENTE);

        this.liberationConfirmee = true;
        this.statut = StatutTransport.ANNULE;
        this.syncEtat = SyncEtat.OK;
    }

    public String getBonId() {
        return bonId;
    }

    public String getCamionId() {
        return camionId;
    }

    public BigDecimal getPoidsKg() {
        return poidsKg;
    }

    public String getDestination() {
        return destination;
    }

    public OffsetDateTime getDepartPrevu() {
        return departPrevu;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void preparerPreuveLivraison(
            String receptionnaire,
            java.time.LocalDate dateReception,
            String commentaire
    ) {
        if (statut != StatutTransport.CLOTURE_EN_ATTENTE
                && statut != StatutTransport.LIVRE) {
            throw new IllegalStateException(
                    "Le transport doit être en attente de clôture"
            );
        }

        if (receptionnaire == null
                || receptionnaire.isBlank()
                || receptionnaire.length() > 160
                || dateReception == null
                || (commentaire != null && commentaire.length() > 1000)) {
            throw new IllegalArgumentException("Données de preuve invalides");
        }

        if (preuveReceptionnaire != null) {
            if (!correspondPreuveLivraison(
                    receptionnaire, dateReception, commentaire)) {
                throw new IllegalStateException(
                        "Une preuve différente a déjà été préparée"
                );
            }
            return;
        }

        if (preuveConfirmee || statut == StatutTransport.LIVRE) {
            throw new IllegalStateException(
                    "Ancienne preuve sans données locales : vérification manuelle nécessaire"
            );
        }

        this.preuveReceptionnaire = receptionnaire;
        this.preuveDateReception = dateReception;
        this.preuveCommentaire = commentaire;
    }

    public boolean correspondPreuveLivraison(
            String receptionnaire,
            java.time.LocalDate dateReception,
            String commentaire
    ) {
        return preuveReceptionnaire != null
                && Objects.equals(preuveReceptionnaire, receptionnaire)
                && Objects.equals(preuveDateReception, dateReception)
                && Objects.equals(preuveCommentaire, commentaire);
    }


}
