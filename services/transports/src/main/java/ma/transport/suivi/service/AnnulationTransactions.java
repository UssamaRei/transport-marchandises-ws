package ma.transport.suivi.service;

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
public class AnnulationTransactions {

    private final TransportRepository repository;

    public AnnulationTransactions(TransportRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TransportResponse commencer(String transportId) {
        Transport transport = trouverAvecVerrou(transportId);

        if (transport.getStatut() == StatutTransport.ANNULE
                || transport.getStatut()
                == StatutTransport.ANNULATION_EN_ATTENTE) {
            return TransportResponse.from(transport);
        }

        if (transport.getStatut() != StatutTransport.PLANIFIE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seul un transport planifié peut être annulé"
            );
        }

        if (transport.getReservationId() == null
                || transport.getReservationId().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Réservation confirmée obligatoire"
            );
        }

        transport.demanderAnnulation();
        return TransportResponse.from(transport);
    }

    @Transactional
    public TransportResponse confirmer(
            String transportId,
            ReservationLiberationResponse liberation
    ) {
        Transport transport = trouverAvecVerrou(transportId);

        if (transport.getStatut() == StatutTransport.ANNULE) {
            return TransportResponse.from(transport);
        }

        if (transport.getStatut()
                != StatutTransport.ANNULATION_EN_ATTENTE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le transport n'est pas en attente d'annulation"
            );
        }

        if (liberation == null
                || !transport.getReservationId().equals(liberation.id())
                || !transport.getId().equals(liberation.transportId())
                || !transport.getCamionId().equals(liberation.camionId())
                || !"LIBEREE".equals(liberation.statut())
                || !"ANNULATION".equals(liberation.motifLiberation())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Confirmation de libération incohérente"
            );
        }

        transport.confirmerLiberationEtFinaliserAnnulation();
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
}
