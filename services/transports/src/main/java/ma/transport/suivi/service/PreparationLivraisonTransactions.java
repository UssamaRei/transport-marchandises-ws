package ma.transport.suivi.service;

import ma.transport.suivi.dto.LivraisonContexte;
import ma.transport.suivi.dto.LivraisonRequest;
import ma.transport.suivi.dto.PreuveLivraisonRequest;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.repository.TransportRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PreparationLivraisonTransactions {

    private final TransportRepository repository;

    public PreparationLivraisonTransactions(TransportRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LivraisonContexte preparer(String transportId, LivraisonRequest demande) {
        if (transportId == null || transportId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Identifiant obligatoire"
            );
        }

        if (demande == null
                || demande.receptionnaire() == null
                || demande.receptionnaire().isBlank()
                || demande.receptionnaire().length() > 160
                || demande.dateReception() == null
                || (demande.commentaire() != null
                && demande.commentaire().length() > 1000)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Données de preuve invalides"
            );
        }

        Transport transport = repository.findByIdForUpdate(transportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transport introuvable"
                ));

        if (transport.getStatut() != StatutTransport.CLOTURE_EN_ATTENTE
                && transport.getStatut() != StatutTransport.LIVRE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'arrivée doit être enregistrée avant la livraison"
            );
        }

        if (transport.getReservationId() == null
                || transport.getReservationId().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Réservation manquante"
            );
        }

        try {
            transport.preparerPreuveLivraison(
                    demande.receptionnaire(),
                    demande.dateReception(),
                    demande.commentaire()
            );
        } catch (IllegalStateException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, erreur.getMessage(), erreur
            );
        }

        PreuveLivraisonRequest preuve = new PreuveLivraisonRequest(
                transport.getId(),
                demande.receptionnaire(),
                demande.dateReception(),
                demande.commentaire(),
                "livraison:" + transport.getId()
        );

        return new LivraisonContexte(TransportResponse.from(transport), preuve);
    }
}