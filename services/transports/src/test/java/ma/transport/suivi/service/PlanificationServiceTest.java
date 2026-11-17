package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ma.transport.suivi.client.CamionsClient;
import ma.transport.suivi.dto.BonDetails;
import ma.transport.suivi.dto.PlanificationRequest;
import ma.transport.suivi.dto.ReservationRequest;
import ma.transport.suivi.dto.ReservationResponse;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.Transport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanificationServiceTest {

    @Mock
    private PlanificationPreparationService preparation;

    @Mock
    private CamionsClient camionsClient;

    @Mock
    private ReservationConfirmationService confirmation;

    private PlanificationService service;
    private PlanificationRequest demande;
    private TransportResponse enPreparation;
    private TransportResponse planifie;
    private ReservationResponse reservation;

    @BeforeEach
    void setUp() {
        service = new PlanificationService(
                preparation, camionsClient, confirmation
        );

        demande = new PlanificationRequest(
                "bon-1",
                "camion-1",
                OffsetDateTime.parse("2026-11-16T08:00:00Z"),
                "operation-1"
        );

        BonDetails bon = new BonDetails(
                "bon-1", "EMIS", "Casablanca",
                new BigDecimal("225.000")
        );

        Transport objet = new TransportPreparationFactory()
                .preparer(demande, bon);

        enPreparation = TransportResponse.from(objet);

        reservation = new ReservationResponse(
                "reservation-1", "camion-1", objet.getId(), "ACTIVE"
        );

        objet.confirmerReservation("reservation-1");
        planifie = TransportResponse.from(objet);
    }

    private ResponseStatusException nonTrouvee() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Réservation introuvable"
        );
    }

    @Test
    void reservePuisConfirmeSiAucuneReservationNexiste() {
        when(preparation.preparerOuRetrouver(demande))
                .thenReturn(enPreparation);
        when(camionsClient.getParTransport(enPreparation.id()))
                .thenThrow(nonTrouvee());
        when(camionsClient.reserver(any(ReservationRequest.class)))
                .thenReturn(reservation);
        when(confirmation.confirmer(enPreparation.id(), reservation))
                .thenReturn(planifie);

        TransportResponse resultat = service.planifier(demande);

        assertSame(planifie, resultat);
        verify(camionsClient).reserver(new ReservationRequest(
                "camion-1", enPreparation.id(), new BigDecimal("225.000")
        ));
        verify(confirmation).confirmer(enPreparation.id(), reservation);
    }

    @Test
    void retrouveLaReservationSansEnCreerUneAutre() {
        when(preparation.preparerOuRetrouver(demande))
                .thenReturn(enPreparation);
        when(camionsClient.getParTransport(enPreparation.id()))
                .thenReturn(reservation);
        when(confirmation.confirmer(enPreparation.id(), reservation))
                .thenReturn(planifie);

        assertSame(planifie, service.planifier(demande));

        verify(camionsClient, never()).reserver(any());
        verify(confirmation).confirmer(enPreparation.id(), reservation);
    }

    @Test
    void transportDejaPlanifieNeReservePasDeNouveau() {
        when(preparation.preparerOuRetrouver(demande))
                .thenReturn(planifie);

        assertSame(planifie, service.planifier(demande));

        verifyNoInteractions(camionsClient, confirmation);
    }

    @Test
    void rechercheInaccessibleNeDeclenchePasDeReservation() {
        when(preparation.preparerOuRetrouver(demande))
                .thenReturn(enPreparation);
        when(camionsClient.getParTransport(enPreparation.id()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Camions inaccessible"
                ));

        assertThrows(
                ResponseStatusException.class,
                () -> service.planifier(demande)
        );

        verify(camionsClient, never()).reserver(any());
        verifyNoInteractions(confirmation);
    }

    @Test
    void repriseApresReponseIncertaineRetrouveLaReservation() {
        when(preparation.preparerOuRetrouver(demande))
                .thenReturn(enPreparation);
        when(camionsClient.getParTransport(enPreparation.id()))
                .thenThrow(nonTrouvee())
                .thenReturn(reservation);
        when(camionsClient.reserver(any(ReservationRequest.class)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Réponse incertaine"
                ));
        when(confirmation.confirmer(enPreparation.id(), reservation))
                .thenReturn(planifie);

        assertThrows(
                ResponseStatusException.class,
                () -> service.planifier(demande)
        );

        assertSame(planifie, service.planifier(demande));

        verify(camionsClient, times(1)).reserver(any());
        verify(confirmation, times(1))
                .confirmer(enPreparation.id(), reservation);
    }
}