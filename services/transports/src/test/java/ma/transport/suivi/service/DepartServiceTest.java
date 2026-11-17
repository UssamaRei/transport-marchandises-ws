package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import ma.transport.suivi.dto.DepartRequest;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.EvenementSuivi;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.model.TypeEvenement;
import ma.transport.suivi.repository.EvenementSuiviRepository;
import ma.transport.suivi.repository.TransportRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartServiceTest {

    private static final OffsetDateTime DATE =
            OffsetDateTime.parse("2026-11-15T10:00:00Z");

    @Mock
    private TransportRepository transportRepository;

    @Mock
    private EvenementSuiviRepository evenementRepository;

    @InjectMocks
    private DepartService service;

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

    @Test
    void departPlanifieCreeEvenement() {
        Transport transport = preparation();
        transport.confirmerReservation("reservation-1");

        when(transportRepository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        TransportResponse resultat = service.declarerDepart(
                transport.getId(), new DepartRequest(DATE)
        );

        assertEquals(StatutTransport.EN_COURS, resultat.statut());
        assertEquals(DATE, resultat.departReel());

        ArgumentCaptor<EvenementSuivi> capture =
                ArgumentCaptor.forClass(EvenementSuivi.class);

        verify(evenementRepository).save(capture.capture());
        assertEquals(TypeEvenement.DEPART, capture.getValue().getType());
        assertEquals(transport.getId(), capture.getValue().getTransportId());
    }

    @Test
    void refuserDepartSansReservationConfirmee() {
        Transport transport = preparation();

        when(transportRepository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.declarerDepart(
                        transport.getId(), new DepartRequest(DATE)
                )
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        assertEquals(StatutTransport.PREPARATION, transport.getStatut());
        verifyNoInteractions(evenementRepository);
    }

    @Test
    void refuserDateDeDepartAbsente() {
        Transport transport = preparation();
        transport.confirmerReservation("reservation-1");

        when(transportRepository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.declarerDepart(
                        transport.getId(), new DepartRequest(null)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
        assertEquals(StatutTransport.PLANIFIE, transport.getStatut());
        verify(evenementRepository, never()).save(any());
    }
}