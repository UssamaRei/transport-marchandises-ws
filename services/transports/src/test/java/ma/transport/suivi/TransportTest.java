package ma.transport.suivi;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;

class TransportTest {
    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-07T10:00:00Z");
    private Transport preparation() {
        return new Transport("bon-1", "camion-1", new BigDecimal("120.000"),
                "Marrakech", T0, "operation-1", T0);
    }
    private Transport planifie() { Transport t = preparation(); t.confirmerReservation("reservation-1"); return t; }
    private Transport enCours() { Transport t = planifie(); t.demarrer(T0); return t; }
    private Transport enCloture() { Transport t = enCours(); t.enregistrerArrivee(T0.plusHours(2)); return t; }

    @Test void reservationConfirmeePlanifieLeTransport() {
        Transport t = preparation(); t.confirmerReservation("reservation-1");
        assertAll(() -> assertEquals(StatutTransport.PLANIFIE, t.getStatut()),
                () -> assertEquals("reservation-1", t.getReservationId()),
                () -> assertEquals(SyncEtat.OK, t.getSyncEtat()));
    }
    @Test void reservationVideRefuseeSansModifierLeTransport() {
        Transport t = preparation();
        assertThrows(IllegalArgumentException.class, () -> t.confirmerReservation("  "));
        assertEquals(StatutTransport.PREPARATION, t.getStatut());
    }
    @Test void departNormalApresReservation() {
        Transport t = planifie(); t.demarrer(T0);
        assertAll(() -> assertEquals(StatutTransport.EN_COURS, t.getStatut()),
                () -> assertEquals(T0, t.getDepartReel()));
    }
    @Test void departSansReservationRefuse() {
        Transport t = preparation();
        assertThrows(IllegalStateException.class, () -> t.demarrer(T0));
        assertAll(() -> assertEquals(StatutTransport.PREPARATION, t.getStatut()), () -> assertNull(t.getDepartReel()));
    }
    @Test void arriveeAuMemeInstantQueDepartAccepteeCasLimite() {
        Transport t = enCours(); t.enregistrerArrivee(T0);
        assertAll(() -> assertEquals(StatutTransport.CLOTURE_EN_ATTENTE, t.getStatut()),
                () -> assertEquals(T0, t.getArriveeReelle()), () -> assertEquals(SyncEtat.A_REPRENDRE, t.getSyncEtat()));
    }
    @Test void arriveeAvantDepartRefuseeSansModification() {
        Transport t = enCours();
        assertThrows(IllegalArgumentException.class, () -> t.enregistrerArrivee(T0.minusNanos(1000)));
        assertAll(() -> assertEquals(StatutTransport.EN_COURS, t.getStatut()), () -> assertNull(t.getArriveeReelle()));
    }
    @Test void livraisonRefuseeSansAucuneConfirmation() {
        Transport t = enCloture();
        assertThrows(IllegalStateException.class, t::finaliserLivraison);
        assertEquals(StatutTransport.CLOTURE_EN_ATTENTE, t.getStatut());
    }
    @Test void livraisonRefuseeSiSeulePreuveConfirmee() {
        Transport t = enCloture(); t.confirmerPreuve();
        assertThrows(IllegalStateException.class, t::finaliserLivraison);
        assertAll(() -> assertEquals(StatutTransport.CLOTURE_EN_ATTENTE, t.getStatut()),
                () -> assertTrue(t.isPreuveConfirmee()), () -> assertFalse(t.isLiberationConfirmee()));
    }
    @Test void livraisonRefuseeSiSeuleLiberationConfirmee() {
        Transport t = enCloture(); t.confirmerLiberation();
        assertThrows(IllegalStateException.class, t::finaliserLivraison);
        assertEquals(StatutTransport.CLOTURE_EN_ATTENTE, t.getStatut());
    }
    @Test void livraisonConfirmeeApresPreuveEtLiberation() {
        Transport t = enCloture(); t.confirmerPreuve(); t.confirmerLiberation(); t.finaliserLivraison();
        assertAll(() -> assertEquals(StatutTransport.LIVRE, t.getStatut()),
                () -> assertEquals(SyncEtat.OK, t.getSyncEtat()));
    }
    @Test void poidsNulRefuse() {
        assertThrows(IllegalArgumentException.class, () -> new Transport("b", "c", BigDecimal.ZERO, "Fès", T0, "k", T0));
    }
}
