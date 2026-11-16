package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import ma.transport.suivi.client.BonClient;
import ma.transport.suivi.dto.BonDetails;
import ma.transport.suivi.dto.PlanificationRequest;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanificationPreparationServiceTest {

    @Mock
    private PreparationTransactions transactions;

    @Mock
    private PreparationService preparationService;

    @Mock
    private BonClient bonClient;

    private PlanificationPreparationService service;
    private PlanificationRequest demande;
    private BonDetails bon;
    private TransportResponse transport;

    @BeforeEach
    void setUp() {
        service = new PlanificationPreparationService(
                transactions, preparationService, bonClient
        );

        demande = new PlanificationRequest(
                "bon-1",
                "camion-1",
                OffsetDateTime.parse("2026-11-16T08:00:00Z"),
                "operation-1"
        );

        bon = new BonDetails(
                "bon-1",
                "EMIS",
                "Casablanca",
                new BigDecimal("225.000")
        );

        Transport objet = new TransportPreparationFactory()
                .preparer(demande, bon);

        transport = TransportResponse.from(objet);
    }

    @Test
    void demandeExistanteEstRetrouveeSansAppelAuBon() {
        when(transactions.retrouver(demande))
                .thenReturn(Optional.of(transport));

        TransportResponse resultat = service.preparerOuRetrouver(demande);

        assertSame(transport, resultat);
        verifyNoInteractions(bonClient, preparationService);
    }

    @Test
    void nouvelleDemandeConsulteLeBonPuisPrepareLeTransport() {
        when(transactions.retrouver(demande))
                .thenReturn(Optional.empty());
        when(bonClient.getBon("bon-1")).thenReturn(bon);
        when(preparationService.preparer(demande, bon))
                .thenReturn(transport);

        TransportResponse resultat = service.preparerOuRetrouver(demande);

        assertSame(transport, resultat);

        var ordre = inOrder(transactions, bonClient, preparationService);
        ordre.verify(transactions).retrouver(demande);
        ordre.verify(bonClient).getBon("bon-1");
        ordre.verify(preparationService).preparer(demande, bon);
    }

    @Test
    void erreurDuServiceBonsNeCreePasDePreparation() {
        when(transactions.retrouver(demande))
                .thenReturn(Optional.empty());
        when(bonClient.getBon("bon-1"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Service Bons inaccessible"
                ));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.preparerOuRetrouver(demande)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, erreur.getStatusCode());
        verifyNoInteractions(preparationService);
    }

    @Test
    void conflitSurLaCleNeConsultePasLeBon() {
        when(transactions.retrouver(demande))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Clé déjà utilisée pour une autre demande"
                ));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.preparerOuRetrouver(demande)
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        verifyNoInteractions(bonClient, preparationService);
    }
}