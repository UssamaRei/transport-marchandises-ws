package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ma.transport.suivi.dto.BonDetails;
import ma.transport.suivi.dto.PlanificationRequest;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.Transport;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class TransportPreparationFactoryTest {

    private static final OffsetDateTime DEPART =
            OffsetDateTime.parse("2026-11-16T08:00:00Z");

    private final TransportPreparationFactory factory =
            new TransportPreparationFactory();

    private PlanificationRequest demande() {
        return new PlanificationRequest(
                "bon-1", "camion-1", DEPART, "operation-1"
        );
    }

    private BonDetails bon(String id, String statut) {
        return new BonDetails(
                id, statut, "Casablanca", new BigDecimal("225.000")
        );
    }

    @Test
    void preparerTransportDepuisBonEmis() {
        Transport resultat = factory.preparer(demande(), bon("bon-1", "EMIS"));

        assertNotNull(resultat.getId());
        assertEquals("bon-1", resultat.getBonId());
        assertEquals("camion-1", resultat.getCamionId());
        assertEquals(StatutTransport.PREPARATION, resultat.getStatut());
        assertEquals(0, new BigDecimal("225").compareTo(resultat.getPoidsKg()));
    }

    @Test
    void refuserBonNonEmis() {
        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> factory.preparer(demande(), bon("bon-1", "LIVRE"))
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
    }

    @Test
    void refuserBonDifferentDeLaDemande() {
        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> factory.preparer(demande(), bon("autre-bon", "EMIS"))
        );

        assertEquals(HttpStatus.BAD_GATEWAY, erreur.getStatusCode());
    }

    @Test
    void refuserCleOperationVide() {
        PlanificationRequest invalide = new PlanificationRequest(
                "bon-1", "camion-1", DEPART, " "
        );

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> factory.preparer(invalide, bon("bon-1", "EMIS"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
    }
}