package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import ma.transport.suivi.dto.ReservationResponse;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.repository.TransportRepository;

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
class ReservationConfirmationServiceTest {

    private static final OffsetDateTime DATE =
            OffsetDateTime.parse("2026-11-16T08:00:00Z");

    @Mock
    private TransportRepository repository;

    @InjectMocks
    private ReservationConfirmationService service;

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

    private ReservationResponse reservation(
            Transport transport,
            String camionId,
            String statut
    ) {
        return new ReservationResponse(
                "reservation-1",
                camionId,
                transport.getId(),
                statut
        );
    }

    @Test
    void confirmationActivePlanifieLeTransport() {
        Transport transport = preparation();

        when(repository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        TransportResponse resultat = service.confirmer(
                transport.getId(),
                reservation(transport, "camion-1", "ACTIVE")
        );

        assertEquals(StatutTransport.PLANIFIE, resultat.statut());
        assertEquals("reservation-1", resultat.reservationId());
    }

    @Test
    void memeConfirmationPeutEtreReprise() {
        Transport transport = preparation();
        transport.confirmerReservation("reservation-1");

        when(repository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        TransportResponse resultat = service.confirmer(
                transport.getId(),
                reservation(transport, "camion-1", "ACTIVE")
        );

        assertEquals(StatutTransport.PLANIFIE, resultat.statut());
        assertEquals("reservation-1", resultat.reservationId());
    }

    @Test
    void refuserReservationPourUnAutreCamion() {
        Transport transport = preparation();

        when(repository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmer(
                        transport.getId(),
                        reservation(transport, "camion-2", "ACTIVE")
                )
        );

        assertEquals(HttpStatus.BAD_GATEWAY, erreur.getStatusCode());
        assertEquals(StatutTransport.PREPARATION, transport.getStatut());
        assertNull(transport.getReservationId());
    }

    @Test
    void refuserReservationNonActive() {
        Transport transport = preparation();

        when(repository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmer(
                        transport.getId(),
                        reservation(transport, "camion-1", "LIBEREE")
                )
        );

        assertEquals(HttpStatus.BAD_GATEWAY, erreur.getStatusCode());
        assertEquals(StatutTransport.PREPARATION, transport.getStatut());
    }
}