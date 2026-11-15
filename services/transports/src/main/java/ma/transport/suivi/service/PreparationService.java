package ma.transport.suivi.service;

import ma.transport.suivi.dto.BonDetails;
import ma.transport.suivi.dto.PlanificationRequest;
import ma.transport.suivi.dto.TransportResponse;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PreparationService {

    private final PreparationTransactions transactions;

    public PreparationService(PreparationTransactions transactions) {
        this.transactions = transactions;
    }

    // Ne pas entourer cette méthode d'une transaction.
    public TransportResponse preparer(
            PlanificationRequest demande,
            BonDetails bon
    ) {
        try {
            return transactions.creerOuRetrouver(demande, bon);

        } catch (DataIntegrityViolationException erreur) {
            // Après l'échec et le rollback, relire dans une nouvelle transaction.
            return transactions.retrouver(demande)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Impossible de créer la préparation : conflit ou données liées invalides"
                    ));
        }
    }
}