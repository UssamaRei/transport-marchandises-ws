package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.EvenementSuivi;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.SyncEtat;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.model.TypeEvenement;
import ma.transport.suivi.repository.EvenementSuiviRepository;
import ma.transport.suivi.repository.TransportRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArriveeServiceTest {

    private static final OffsetDateTime DEPART =
            OffsetDateTime.parse("2026-11-16T08:00:00Z");

    @Mock
    private TransportRepository transportRepository;

    @Mock
    private EvenementSuiviRepository evenementRepository;

    private ArriveeService service;
    private Transport transport;

    @BeforeEach
    void setUp() {
        service = new ArriveeService(
                transportRepository, evenementRepository
        );

        transport = new Transport(
                "bon-1",
                "camion-1",
                new BigDecimal("225.000"),
                "Casablanca",
                DEPART,
                "operation-1",
                DEPART
        );
        transport.confirmerReservation("reservation-1");
        transport.demarrer(DEPART);

        when(transportRepository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));
    }

    @Test
    void arriveeEnregistreUnEvenementEtMetLaClotureEnAttente() {
        OffsetDateTime arrivee = DEPART.plusHours(3);

        TransportResponse resultat =
                service.enregistrer(transport.getId(), arrivee);

        assertEquals(StatutTransport.CLOTURE_EN_ATTENTE, resultat.statut());
        assertEquals(SyncEtat.A_REPRENDRE, resultat.syncEtat());
        assertTrue(arrivee.isEqual(resultat.arriveeReelle()));

        ArgumentCaptor<EvenementSuivi> capture =
                ArgumentCaptor.forClass(EvenementSuivi.class);

        verify(evenementRepository).save(capture.capture());
        assertEquals(TypeEvenement.ARRIVEE, capture.getValue().getType());
        assertEquals(transport.getId(), capture.getValue().getTransportId());
    }

    @Test
    void repetitionDeLaMemeArriveeNeCreePasDeuxEvenements() {
        OffsetDateTime arrivee = DEPART.plusHours(3);

        service.enregistrer(transport.getId(), arrivee);
        service.enregistrer(transport.getId(), arrivee);

        verify(evenementRepository, times(1))
                .save(any(EvenementSuivi.class));
    }

    @Test
    void dateAvantDepartEstRefuseeSansModification() {
        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.enregistrer(
                        transport.getId(), DEPART.minusSeconds(1)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
        assertEquals(StatutTransport.EN_COURS, transport.getStatut());
        assertNull(transport.getArriveeReelle());
        verifyNoInteractions(evenementRepository);
    }

    @Test
    void dateDifferenteApresArriveeEstRefusee() {
        service.enregistrer(transport.getId(), DEPART.plusHours(3));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.enregistrer(
                        transport.getId(), DEPART.plusHours(4)
                )
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        verify(evenementRepository, times(1))
                .save(any(EvenementSuivi.class));
    }

    @Test
    void arriveeAuMemeInstantQueLeDepartEstAcceptee() {
        TransportResponse resultat =
                service.enregistrer(transport.getId(), DEPART);

        assertEquals(StatutTransport.CLOTURE_EN_ATTENTE, resultat.statut());
        assertTrue(DEPART.isEqual(resultat.arriveeReelle()));
    }
}