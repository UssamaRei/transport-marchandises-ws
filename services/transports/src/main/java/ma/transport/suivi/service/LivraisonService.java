package ma.transport.suivi.service;

import ma.transport.suivi.client.LiberationCamionsClient;
import ma.transport.suivi.client.PreuveLivraisonClient;
import ma.transport.suivi.dto.LivraisonContexte;
import ma.transport.suivi.dto.LivraisonRequest;
import ma.transport.suivi.dto.PreuveLivraisonResponse;
import ma.transport.suivi.dto.ReservationLiberationResponse;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.StatutTransport;

import org.springframework.stereotype.Service;

@Service
public class LivraisonService {

    private final PreparationLivraisonTransactions preparation;
    private final PreuveLivraisonClient preuveClient;
    private final LiberationCamionsClient camionsClient;
    private final LivraisonTransactions confirmations;

    public LivraisonService(
            PreparationLivraisonTransactions preparation,
            PreuveLivraisonClient preuveClient,
            LiberationCamionsClient camionsClient,
            LivraisonTransactions confirmations
    ) {
        this.preparation = preparation;
        this.preuveClient = preuveClient;
        this.camionsClient = camionsClient;
        this.confirmations = confirmations;
    }

    public TransportResponse livrer(String transportId, LivraisonRequest demande) {
        LivraisonContexte contexte = preparation.preparer(transportId, demande);
        TransportResponse etat = contexte.transport();

        if (etat.statut() == StatutTransport.LIVRE) {
            return etat;
        }

        if (!etat.preuveConfirmee()) {
            PreuveLivraisonResponse preuve = preuveClient.confirmer(
                    etat.bonId(), contexte.preuve()
            );

            etat = confirmations.confirmerPreuve(
                    transportId, contexte.preuve(), preuve
            );
        }

        // Une autre tentative peut avoir terminé entre-temps.
        if (etat.statut() == StatutTransport.LIVRE) {
            return etat;
        }

        ReservationLiberationResponse liberation =
                camionsClient.libererPourLivraison(etat.reservationId());

        return confirmations.confirmerLiberationEtFinaliser(
                transportId, liberation
        );
    }
}