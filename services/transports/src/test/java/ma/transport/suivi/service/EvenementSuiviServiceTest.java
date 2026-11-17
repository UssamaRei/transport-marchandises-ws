package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import ma.transport.suivi.dto.EvenementRequest;
import ma.transport.suivi.dto.EvenementResponse;
import ma.transport.suivi.model.EvenementSuivi;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.model.TypeEvenement;
import ma.transport.suivi.repository.EvenementSuiviRepository;
import ma.transport.suivi.repository.TransportRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvenementSuiviServiceTest {

    private static final OffsetDateTime DEPART =
            OffsetDateTime.parse("2026-11-15T10:00:00Z");

    @Mock
    private TransportRepository transportRepository;

    @Mock
    private EvenementSuiviRepository evenementRepository;

    @InjectMocks
    private EvenementSuiviService service;

    private Transport transportEnPreparation() {
        return new Transport(
                "bon-1",
                "camion-1",
                new BigDecimal("225.000"),
                "Casablanca",
                DEPART,
                "operation-1",
                DEPART
        );
    }

    private Transport transportEnCours() {
        Transport transport = transportEnPreparation();
        transport.confirmerReservation("reservation-1");
        transport.demarrer(DEPART);
        return transport;
    }

    private EvenementRequest pointage(OffsetDateTime date) {
        return new EvenementRequest(
                TypeEvenement.POINTAGE,
                "Passage à Marrakech",
                date,
                null,
                null
        );
    }

    @Test
    void ajouterPointageValide() {
        Transport transport = transportEnCours();
        String id = transport.getId();

        when(transportRepository.findByIdForUpdate(id))
                .thenReturn(Optional.of(transport));
        when(evenementRepository.save(any(EvenementSuivi.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvenementResponse resultat = service.ajouter(
                id, pointage(DEPART.plusHours(1))
        );

        assertEquals(id, resultat.transportId());
        assertEquals(TypeEvenement.POINTAGE, resultat.type());
        assertEquals("Passage à Marrakech", resultat.description());
        verify(evenementRepository).save(any(EvenementSuivi.class));
    }

    @Test
    void refuserTransportInexistant() {
        when(transportRepository.findByIdForUpdate("absent"))
                .thenReturn(Optional.empty());

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.ajouter("absent", pointage(DEPART))
        );

        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatusCode());
        verifyNoInteractions(evenementRepository);
    }

    @Test
    void refuserEvenementSiTransportPasEnCours() {
        Transport transport = transportEnPreparation();

        when(transportRepository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.ajouter(transport.getId(), pointage(DEPART))
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        verifyNoInteractions(evenementRepository);
    }

    @Test
    void refuserEvenementAvantDepart() {
        Transport transport = transportEnCours();

        when(transportRepository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.ajouter(
                        transport.getId(), pointage(DEPART.minusSeconds(1))
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
        verifyNoInteractions(evenementRepository);
    }

    @Test
    void refuserTypeDepartDansEndpointEvenements() {
        Transport transport = transportEnCours();

        when(transportRepository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        EvenementRequest demande = new EvenementRequest(
                TypeEvenement.DEPART,
                "Départ",
                DEPART,
                null,
                null
        );

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.ajouter(transport.getId(), demande)
        );

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
        verifyNoInteractions(evenementRepository);
    }

    @Test
    void refuserDescriptionVide() {
        Transport transport = transportEnCours();

        when(transportRepository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        EvenementRequest demande = new EvenementRequest(
                TypeEvenement.COMMENTAIRE,
                "   ",
                DEPART,
                null,
                null
        );

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.ajouter(transport.getId(), demande)
        );

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
        verifyNoInteractions(evenementRepository);
    }

    @Test
    void refuserLongitudeInvalide() {
        Transport transport = transportEnCours();

        when(transportRepository.findByIdForUpdate(transport.getId()))
                .thenReturn(Optional.of(transport));

        EvenementRequest demande = new EvenementRequest(
                TypeEvenement.POINTAGE,
                "Position",
                DEPART,
                null,
                new BigDecimal("181")
        );

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.ajouter(transport.getId(), demande)
        );

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
        verifyNoInteractions(evenementRepository);
    }
}