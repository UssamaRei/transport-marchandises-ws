package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import ma.transport.suivi.client.LiberationCamionsClient;
import ma.transport.suivi.dto.ReservationLiberationResponse;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.SyncEtat;
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
class AnnulationServiceTest {

    @Mock
    private TransportRepository repository;

    @Mock
    private LiberationCamionsClient client;

    private AnnulationService service;
    private Transport transport;
    private ReservationLiberationResponse liberation;

    @BeforeEach
    void setUp() {
        OffsetDateTime date =
                OffsetDateTime.parse("2026-11-16T08:00:00Z");

        transport = new Transport(
                "bon-1", "camion-1", new BigDecimal("225.000"),
                "Casablanca", date, "operation-1", date
        );
        transport.confirmerReservation("reservation-1");

        liberation = new ReservationLiberationResponse(
                "reservation-1",
                "camion-1",
                transport.getId(),
                "LIBEREE",
                "ANNULATION"
        );

        when(repository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        AnnulationTransactions transactions =
                new AnnulationTransactions(repository);

        service = new AnnulationService(transactions, client);
    }

    @Test
    void annuleApresConfirmationEtNeLiberePasDeuxFois() {
        when(client.libererPourAnnulation("reservation-1"))
                .thenReturn(liberation);

        TransportResponse resultat = service.annuler(transport.getId());
        TransportResponse repetition = service.annuler(transport.getId());

        assertEquals(StatutTransport.ANNULE, resultat.statut());
        assertEquals(SyncEtat.OK, resultat.syncEtat());
        assertEquals(StatutTransport.ANNULE, repetition.statut());
        verify(client, times(1))
                .libererPourAnnulation("reservation-1");
    }

    @Test
    void erreurReseauLaisseEnAttentePuisPermetReprise() {
        when(client.libererPourAnnulation("reservation-1"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Réponse incertaine"
                ))
                .thenReturn(liberation);

        assertThrows(
                ResponseStatusException.class,
                () -> service.annuler(transport.getId())
        );

        assertEquals(StatutTransport.ANNULATION_EN_ATTENTE,
                transport.getStatut());
        assertEquals(SyncEtat.A_REPRENDRE, transport.getSyncEtat());

        TransportResponse resultat = service.annuler(transport.getId());

        assertEquals(StatutTransport.ANNULE, resultat.statut());
        verify(client, times(2))
                .libererPourAnnulation("reservation-1");
    }

    @Test
    void confirmationIncoherenteNeFinalisePas() {
        ReservationLiberationResponse mauvaise =
                new ReservationLiberationResponse(
                        "reservation-1", "autre-camion",
                        transport.getId(), "LIBEREE", "ANNULATION"
                );

        when(client.libererPourAnnulation("reservation-1"))
                .thenReturn(mauvaise);

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.annuler(transport.getId())
        );

        assertEquals(HttpStatus.BAD_GATEWAY, erreur.getStatusCode());
        assertEquals(StatutTransport.ANNULATION_EN_ATTENTE,
                transport.getStatut());
    }

    @Test
    void transportPartiNePeutPasEtreAnnule() {
        transport.demarrer(
                OffsetDateTime.parse("2026-11-16T08:00:00Z")
        );

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.annuler(transport.getId())
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        assertEquals(StatutTransport.EN_COURS, transport.getStatut());
        verifyNoInteractions(client);
    }
}