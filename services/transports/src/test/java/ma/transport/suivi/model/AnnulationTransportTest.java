package ma.transport.suivi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnnulationTransportTest {

    private static final OffsetDateTime DATE =
            OffsetDateTime.parse("2026-11-16T08:00:00Z");

    private Transport preparation() {
        return new Transport(
                "bon-1",
                "camion-1",
                new BigDecimal("225.000"),
                "Casablanca",
                DATE,
                "operation-1",
                DATE
        );
    }

    private Transport planifie() {
        Transport transport = preparation();
        transport.confirmerReservation("reservation-1");
        return transport;
    }

    @Test
    void annulationPasseEnAttenteEtPeutEtreRepetee() {
        Transport transport = planifie();

        transport.demanderAnnulation();
        transport.demanderAnnulation();

        assertEquals(StatutTransport.ANNULATION_EN_ATTENTE,
                transport.getStatut());
        assertEquals(SyncEtat.A_REPRENDRE, transport.getSyncEtat());
        assertFalse(transport.isLiberationConfirmee());
    }

    @Test
    void confirmationFinaliseAnnulationEtPeutEtreRepetee() {
        Transport transport = planifie();
        transport.demanderAnnulation();

        transport.confirmerLiberationEtFinaliserAnnulation();
        transport.confirmerLiberationEtFinaliserAnnulation();

        assertEquals(StatutTransport.ANNULE, transport.getStatut());
        assertEquals(SyncEtat.OK, transport.getSyncEtat());
        assertTrue(transport.isLiberationConfirmee());
    }

    @Test
    void annulationApresDepartEstRefusee() {
        Transport transport = planifie();
        transport.demarrer(DATE);

        assertThrows(IllegalStateException.class,
                transport::demanderAnnulation);

        assertEquals(StatutTransport.EN_COURS, transport.getStatut());
    }

    @Test
    void finalisationSansDemandeEstRefusee() {
        Transport transport = planifie();

        assertThrows(IllegalStateException.class,
                transport::confirmerLiberationEtFinaliserAnnulation);

        assertEquals(StatutTransport.PLANIFIE, transport.getStatut());
        assertFalse(transport.isLiberationConfirmee());
    }

    @Test
    void annulationPendantPreparationEstRefuseePourLeMoment() {
        Transport transport = preparation();

        assertThrows(IllegalStateException.class,
                transport::demanderAnnulation);

        assertEquals(StatutTransport.PREPARATION, transport.getStatut());
    }
}