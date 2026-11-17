package ma.transport.suivi.service;

import java.util.Objects;

import ma.transport.suivi.dto.PreuveLivraisonRequest;
import ma.transport.suivi.dto.PreuveLivraisonResponse;
import ma.transport.suivi.dto.ReservationLiberationResponse;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.repository.TransportRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LivraisonTransactions {

    private final TransportRepository repository;

    public LivraisonTransactions(TransportRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TransportResponse confirmerPreuve(
            String transportId,
            PreuveLivraisonRequest demande,
            PreuveLivraisonResponse preuve
    ) {
        Transport transport = trouverAvecVerrou(transportId);
        verifierEtatLivraison(transport);

        // Vérifie que la demande correspond à la preuve préparée localement.
        if (demande == null
                || !transport.correspondPreuveLivraison(
                demande.receptionnaire(),
                demande.dateReception(),
                demande.commentaire()
        )
                || !("livraison:" + transport.getId()).equals(
                demande.operationKey()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La preuve ne correspond pas aux données préparées"
            );
        }

        // Vérifie la confirmation renvoyée par le service Bons.
        if (preuve == null
                || demande.transportId() == null
                || demande.receptionnaire() == null
                || demande.receptionnaire().isBlank()
                || demande.dateReception() == null
                || !transport.getId().equals(demande.transportId())
                || !transport.getId().equals(preuve.transportId())
                || !transport.getBonId().equals(preuve.bonId())
                || !demande.receptionnaire().equals(preuve.receptionnaire())
                || !demande.dateReception().equals(preuve.dateReception())
                || !Objects.equals(demande.commentaire(), preuve.commentaire())
                || !"LIVRE".equals(preuve.statut())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Confirmation de preuve incohérente"
            );
        }

        if (transport.getStatut() == StatutTransport.LIVRE) {
            return TransportResponse.from(transport);
        }

        if (!transport.isPreuveConfirmee()) {
            transport.confirmerPreuve();
        }

        return TransportResponse.from(transport);
    }

    @Transactional
    public TransportResponse confirmerLiberationEtFinaliser(
            String transportId,
            ReservationLiberationResponse liberation
    ) {
        Transport transport = trouverAvecVerrou(transportId);
        verifierEtatLivraison(transport);

        if (liberation == null
                || !Objects.equals(
                transport.getReservationId(), liberation.id())
                || !transport.getId().equals(liberation.transportId())
                || !transport.getCamionId().equals(liberation.camionId())
                || !"LIBEREE".equals(liberation.statut())
                || !"LIVRAISON".equals(liberation.motifLiberation())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Confirmation de libération incohérente"
            );
        }

        if (transport.getStatut() == StatutTransport.LIVRE) {
            return TransportResponse.from(transport);
        }

        if (!transport.isPreuveConfirmee()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La preuve de réception doit être confirmée"
            );
        }

        if (!transport.isLiberationConfirmee()) {
            transport.confirmerLiberation();
        }

        transport.finaliserLivraison();
        return TransportResponse.from(transport);
    }

    private Transport trouverAvecVerrou(String transportId) {
        if (transportId == null || transportId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identifiant du transport obligatoire"
            );
        }

        return repository.findByIdForUpdate(transportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transport introuvable"
                ));
    }

    private void verifierEtatLivraison(Transport transport) {
        if (transport.getStatut() != StatutTransport.CLOTURE_EN_ATTENTE
                && transport.getStatut() != StatutTransport.LIVRE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le transport doit être en attente de clôture"
            );
        }
    }
}