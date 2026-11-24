package ma.transport.suivi.service;

import java.util.Optional;

import ma.transport.suivi.dto.BonDetails;
import ma.transport.suivi.dto.PlanificationRequest;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.repository.TransportRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PreparationTransactions {

    private final TransportRepository repository;
    private final TransportPreparationFactory factory;

    public PreparationTransactions(
            TransportRepository repository,
            TransportPreparationFactory factory
    ) {
        this.repository = repository;
        this.factory = factory;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransportResponse creerOuRetrouver(
            PlanificationRequest demande,
            BonDetails bon
    ) {
        validerDemande(demande);

        Optional<Transport> existant =
                repository.findByOperationKey(demande.operationKey());

        if (existant.isPresent()) {
            return verifierEtConvertir(existant.get(), demande);
        }

        Transport transport = factory.preparer(demande, bon);
        Transport enregistre = repository.saveAndFlush(transport);

        return TransportResponse.from(enregistre);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<TransportResponse> retrouver(PlanificationRequest demande) {
        validerDemande(demande);

        return repository.findByOperationKey(demande.operationKey())
                .map(transport -> verifierEtConvertir(transport, demande));
    }

    private TransportResponse verifierEtConvertir(
            Transport transport,
            PlanificationRequest demande
    ) {
        boolean memeDemande =
                transport.getBonId().equals(demande.bonId())
                        && transport.getCamionId().equals(demande.camionId())
                        && transport.getDepartPrevu().isEqual(demande.departPrevu());

        if (!memeDemande) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette clé d'opération est déjà utilisée pour une autre demande"
            );
        }

        return TransportResponse.from(transport);
    }

    private void validerDemande(PlanificationRequest demande) {
        if (demande == null
                || invalide(demande.bonId(), 64)
                || invalide(demande.camionId(), 64)
                || invalide(demande.operationKey(), 160)
                || demande.departPrevu() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bon, camion, départ prévu et clé d'opération sont obligatoires"
            );
        }
    }

    private boolean invalide(String valeur, int maximum) {
        return valeur == null || valeur.isBlank() || valeur.length() > maximum;
    }
}
