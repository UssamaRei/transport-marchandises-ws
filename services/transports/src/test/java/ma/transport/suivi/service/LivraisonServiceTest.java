package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import ma.transport.suivi.client.LiberationCamionsClient;
import ma.transport.suivi.client.PreuveLivraisonClient;
import ma.transport.suivi.dto.LivraisonContexte;
import ma.transport.suivi.dto.LivraisonRequest;
import ma.transport.suivi.dto.PreuveLivraisonRequest;
import ma.transport.suivi.dto.PreuveLivraisonResponse;
import ma.transport.suivi.dto.ReservationLiberationResponse;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.Transport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LivraisonServiceTest {

    @Mock
    private PreparationLivraisonTransactions preparation;

    @Mock
    private PreuveLivraisonClient preuveClient;

    @Mock
    private LiberationCamionsClient camionsClient;

    @Mock
    private LivraisonTransactions confirmations;

    @InjectMocks
    private LivraisonService service;

    @Test
    void echecDePreuvePuisNouvelleTentativeTermineLaLivraison() {
        OffsetDateTime date = OffsetDateTime.parse("2026-11-17T08:00:00Z");
        LocalDate dateReception = LocalDate.parse("2026-11-17");

        Transport transport = new Transport(
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
        transport.preparerPreuveLivraison("Mohamed", dateReception, "Reçu");

        String id = transport.getId();

        LivraisonRequest demande =
                new LivraisonRequest("Mohamed", dateReception, "Reçu");

        PreuveLivraisonRequest requetePreuve = new PreuveLivraisonRequest(
                id, "Mohamed", dateReception, "Reçu", "livraison:" + id
        );

        LivraisonContexte contexte = new LivraisonContexte(
                TransportResponse.from(transport),
                requetePreuve
        );

        PreuveLivraisonResponse preuve = new PreuveLivraisonResponse(
                "bon-1", id, "Mohamed", dateReception, "Reçu", "LIVRE"
        );

        ReservationLiberationResponse liberation =
                new ReservationLiberationResponse(
                        "reservation-1", "camion-1", id, "LIBEREE", "LIVRAISON"
                );

        when(preparation.preparer(id, demande)).thenReturn(contexte);

        when(preuveClient.confirmer("bon-1", requetePreuve))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "Bons indisponible"
                ))
                .thenReturn(preuve);

        when(confirmations.confirmerPreuve(id, requetePreuve, preuve))
                .thenAnswer(invocation -> {
                    transport.confirmerPreuve();
                    return TransportResponse.from(transport);
                });

        when(camionsClient.libererPourLivraison("reservation-1"))
                .thenReturn(liberation);

        when(confirmations.confirmerLiberationEtFinaliser(id, liberation))
                .thenAnswer(invocation -> {
                    transport.confirmerLiberation();
                    transport.finaliserLivraison();
                    return TransportResponse.from(transport);
                });

        // Première tentative : Bons échoue.
        assertThrows(
                ResponseStatusException.class,
                () -> service.livrer(id, demande)
        );

        verifyNoInteractions(camionsClient);
        assertEquals(StatutTransport.CLOTURE_EN_ATTENTE, transport.getStatut());

        // Deuxième tentative : même preuve, puis libération.
        TransportResponse resultat = service.livrer(id, demande);

        assertEquals(StatutTransport.LIVRE, resultat.statut());
        verify(preuveClient, times(2)).confirmer("bon-1", requetePreuve);
        verify(camionsClient).libererPourLivraison("reservation-1");
    }
}