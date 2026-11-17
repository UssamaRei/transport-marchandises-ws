package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import ma.transport.suivi.dto.PreuveLivraisonRequest;
import ma.transport.suivi.dto.PreuveLivraisonResponse;
import ma.transport.suivi.dto.ReservationLiberationResponse;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.repository.TransportRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LivraisonTransactionsTest {

    @Mock
    private TransportRepository repository;

    private LivraisonTransactions transactions;
    private Transport transport;
    private PreuveLivraisonRequest demande;
    private PreuveLivraisonResponse preuve;
    private ReservationLiberationResponse liberation;

    @BeforeEach
    void preparer() {
        OffsetDateTime date =
                OffsetDateTime.parse("2026-11-17T08:00:00Z");

        transport = new Transport(
                "bon-1",
                "camion-1",
                new BigDecimal("225.000"),
                "Casablanca",
                date,
                "planification-1",
                date
        );

        transport.confirmerReservation("reservation-1");
        transport.demarrer(date);
        transport.enregistrerArrivee(date.plusHours(3));

        demande = new PreuveLivraisonRequest(
                transport.getId(),
                "Mohamed",
                LocalDate.parse("2026-11-17"),
                "Reçu",
                "preuve-1"
        );

        preuve = new PreuveLivraisonResponse(
                "bon-1",
                transport.getId(),
                "Mohamed",
                LocalDate.parse("2026-11-17"),
                "Reçu",
                "LIVRE"
        );

        liberation = new ReservationLiberationResponse(
                "reservation-1",
                "camion-1",
                transport.getId(),
                "LIBEREE",
                "LIVRAISON"
        );

        when(repository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        transactions = new LivraisonTransactions(repository);
    }

    @Test
    void preuvePuisLiberationCloturentLeTransport() {
        transactions.confirmerPreuve(transport.getId(), demande, preuve);

        assertTrue(transport.isPreuveConfirmee());
        assertEquals(
                StatutTransport.CLOTURE_EN_ATTENTE,
                transport.getStatut()
        );

        transactions.confirmerLiberationEtFinaliser(
                transport.getId(), liberation
        );

        assertTrue(transport.isLiberationConfirmee());
        assertEquals(StatutTransport.LIVRE, transport.getStatut());
    }

    @Test
    void liberationSansPreuveNeCloturePas() {
        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> transactions.confirmerLiberationEtFinaliser(
                        transport.getId(), liberation
                )
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        assertFalse(transport.isLiberationConfirmee());
        assertEquals(
                StatutTransport.CLOTURE_EN_ATTENTE,
                transport.getStatut()
        );
    }

    @Test
    void preuvePourUnAutreTransportEstRefusee() {
        PreuveLivraisonResponse mauvaisePreuve =
                new PreuveLivraisonResponse(
                        "bon-1",
                        "autre-transport",
                        "Mohamed",
                        LocalDate.parse("2026-11-17"),
                        "Reçu",
                        "LIVRE"
                );

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> transactions.confirmerPreuve(
                        transport.getId(), demande, mauvaisePreuve
                )
        );

        assertEquals(HttpStatus.BAD_GATEWAY, erreur.getStatusCode());
        assertFalse(transport.isPreuveConfirmee());
    }

    @Test
    void mauvaiseReservationNeCloturePas() {
        transactions.confirmerPreuve(transport.getId(), demande, preuve);

        ReservationLiberationResponse mauvaiseLiberation =
                new ReservationLiberationResponse(
                        "autre-reservation",
                        "camion-1",
                        transport.getId(),
                        "LIBEREE",
                        "LIVRAISON"
                );

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> transactions.confirmerLiberationEtFinaliser(
                        transport.getId(), mauvaiseLiberation
                )
        );

        assertEquals(HttpStatus.BAD_GATEWAY, erreur.getStatusCode());
        assertFalse(transport.isLiberationConfirmee());
        assertEquals(
                StatutTransport.CLOTURE_EN_ATTENTE,
                transport.getStatut()
        );
    }
}