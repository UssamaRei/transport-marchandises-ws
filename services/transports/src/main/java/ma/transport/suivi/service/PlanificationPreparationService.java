package ma.transport.suivi.service;

import java.util.Optional;

import ma.transport.suivi.client.BonClient;
import ma.transport.suivi.dto.BonDetails;
import ma.transport.suivi.dto.PlanificationRequest;
import ma.transport.suivi.dto.TransportResponse;

import org.springframework.stereotype.Service;

@Service
public class PlanificationPreparationService {

    private final PreparationTransactions transactions;
    private final PreparationService preparationService;
    private final BonClient bonClient;

    public PlanificationPreparationService(
            PreparationTransactions transactions,
            PreparationService preparationService,
            BonClient bonClient
    ) {
        this.transactions = transactions;
        this.preparationService = preparationService;
        this.bonClient = bonClient;
    }

    // Pas de transaction globale : l'appel HTTP reste hors transaction DB.
    public TransportResponse preparerOuRetrouver(
            PlanificationRequest demande
    ) {
        Optional<TransportResponse> existant = transactions.retrouver(demande);

        if (existant.isPresent()) {
            return existant.get();
        }

        BonDetails bon = bonClient.getBon(demande.bonId());
        return preparationService.preparer(demande, bon);
    }
}
