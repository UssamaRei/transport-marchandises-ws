package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import ma.transport.suivi.dto.LivraisonRequest;
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
class PreparationLivraisonTransactionsTest {

    @Mock
    private TransportRepository repository;

    private PreparationLivraisonTransactions service;
    private Transport transport;

    @BeforeEach
    void preparer() {
        OffsetDateTime date = OffsetDateTime.parse("2026-11-17T08:00:00Z");

        transport = new Transport(
                "bon-1", "camion-1", new BigDecimal("225.000"),
                "Casablanca", date, "planification-1", date
        );
        transport.confirmerReservation("reservation-1");
        transport.demarrer(date);
        transport.enregistrerArrivee(date.plusHours(3));

        when(repository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        service = new PreparationLivraisonTransactions(repository);
    }

    @Test
    void memePreuvePeutEtrePrepareeDeuxFois() {
        LivraisonRequest demande = new LivraisonRequest(
                "Mohamed", LocalDate.parse("2026-11-17"), "Reçu"
        );

        var premier = service.preparer(transport.getId(), demande);
        var second = service.preparer(transport.getId(), demande);

        assertEquals("livraison:" + transport.getId(),
                premier.preuve().operationKey());
        assertEquals(premier.preuve(), second.preuve());
    }

    @Test
    void preuveDifferenteEstRefusee() {
        service.preparer(transport.getId(), new LivraisonRequest(
                "Mohamed", LocalDate.parse("2026-11-17"), "Reçu"
        ));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.preparer(transport.getId(), new LivraisonRequest(
                        "Ali", LocalDate.parse("2026-11-17"), "Reçu"
                ))
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        assertTrue(transport.correspondPreuveLivraison(
                "Mohamed", LocalDate.parse("2026-11-17"), "Reçu"
        ));
    }
}