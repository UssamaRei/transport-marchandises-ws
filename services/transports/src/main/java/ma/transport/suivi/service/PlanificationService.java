package ma.transport.suivi.service;

import ma.transport.suivi.client.CamionsClient;
import ma.transport.suivi.dto.PlanificationRequest;
import ma.transport.suivi.dto.ReservationRequest;
import ma.transport.suivi.dto.ReservationResponse;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.StatutTransport;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlanificationService {

    private final PlanificationPreparationService preparation;
    private final CamionsClient camionsClient;
    private final ReservationConfirmationService confirmation;

    public PlanificationService(
            PlanificationPreparationService preparation,
            CamionsClient camionsClient,
            ReservationConfirmationService confirmation
    ) {
        this.preparation = preparation;
        this.camionsClient = camionsClient;
        this.confirmation = confirmation;
    }

    // Pas de transaction globale pendant les appels HTTP.
    public TransportResponse planifier(PlanificationRequest demande) {
        TransportResponse transport =
                preparation.preparerOuRetrouver(demande);

        // Une répétition ne doit pas réserver à nouveau un transport
        // qui a déjà quitté l'état PREPARATION.
        if (transport.statut() != StatutTransport.PREPARATION) {
            return transport;
        }

        ReservationResponse reservation;

        try {
            reservation = camionsClient.getParTransport(transport.id());
        } catch (ResponseStatusException erreur) {
            if (erreur.getStatusCode().value()
                    != HttpStatus.NOT_FOUND.value()) {
                // Résultat inconnu : ne pas tenter une nouvelle réservation.
                throw erreur;
            }

            // Seulement si le contrat Camions garantit que 404 signifie
            // réellement « aucune réservation pour ce transport ».
            reservation = camionsClient.reserver(
                    new ReservationRequest(
                            transport.camionId(),
                            transport.id(),
                            transport.poidsKg()
                    )
            );
        }

        // Ce service vérifie notamment le transport, le camion et ACTIVE.
        return confirmation.confirmer(transport.id(), reservation);
    }
}
