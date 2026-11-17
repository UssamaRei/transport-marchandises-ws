package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import ma.transport.suivi.dto.BonDetails;
import ma.transport.suivi.dto.PlanificationRequest;
import ma.transport.suivi.dto.TransportResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreparationTransactionsTest {

    private static final OffsetDateTime DEPART =
            OffsetDateTime.parse("2026-11-16T08:00:00Z");

    @Mock
    private TransportRepository repository;

    private PreparationTransactions transactions;
    private TransportPreparationFactory factory;

    @BeforeEach
    void preparer() {
        factory = new TransportPreparationFactory();
        transactions = new PreparationTransactions(repository, factory);
    }

    private PlanificationRequest demande(String camionId) {
        return new PlanificationRequest(
                "bon-1", camionId, DEPART, "operation-1"
        );
    }

    private BonDetails bon() {
        return new BonDetails(
                "bon-1", "EMIS", "Casablanca", new BigDecimal("225.000")
        );
    }

    @Test
    void nouvelleDemandeEnregistreUnePreparation() {
        when(repository.findByOperationKey("operation-1"))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Transport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransportResponse resultat =
                transactions.creerOuRetrouver(demande("camion-1"), bon());

        assertNotNull(resultat.id());
        assertEquals(StatutTransport.PREPARATION, resultat.statut());
        verify(repository).saveAndFlush(any(Transport.class));
    }

    @Test
    void memeDemandeRetrouveLeMemeTransport() {
        Transport existant = factory.preparer(demande("camion-1"), bon());

        when(repository.findByOperationKey("operation-1"))
                .thenReturn(Optional.of(existant));

        TransportResponse resultat =
                transactions.creerOuRetrouver(demande("camion-1"), bon());

        assertEquals(existant.getId(), resultat.id());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void memeCleAvecAutreCamionEstRefusee() {
        Transport existant = factory.preparer(demande("camion-1"), bon());

        when(repository.findByOperationKey("operation-1"))
                .thenReturn(Optional.of(existant));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> transactions.creerOuRetrouver(demande("camion-2"), bon())
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        verify(repository, never()).saveAndFlush(any());
    }
}