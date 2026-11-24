package ma.transport.suivi.service;

import ma.transport.suivi.client.LiberationCamionsClient;
import ma.transport.suivi.dto.ReservationLiberationResponse;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.StatutTransport;

import org.springframework.stereotype.Service;

@Service
public class AnnulationService {

    private final AnnulationTransactions transactions;
    private final LiberationCamionsClient client;

    public AnnulationService(
            AnnulationTransactions transactions,
            LiberationCamionsClient client
    ) {
        this.transactions = transactions;
        this.client = client;
    }

    // Pas de transaction globale autour de l'appel HTTP.
    public TransportResponse annuler(String transportId) {
        TransportResponse transport = transactions.commencer(transportId);

        if (transport.statut() == StatutTransport.ANNULE) {
            return transport;
        }

        ReservationLiberationResponse liberation =
                client.libererPourAnnulation(transport.reservationId());

        return transactions.confirmer(transportId, liberation);
    }
}
