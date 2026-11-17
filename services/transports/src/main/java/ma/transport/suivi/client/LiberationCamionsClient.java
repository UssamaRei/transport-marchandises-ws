package ma.transport.suivi.client;

import java.util.Map;

import ma.transport.suivi.dto.ReservationLiberationResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LiberationCamionsClient {

    private final RestClient restClient;

    public LiberationCamionsClient(
            @Qualifier("camionsRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public ReservationLiberationResponse libererPourAnnulation(
            String reservationId
    ) {
        if (reservationId == null || reservationId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identifiant de réservation obligatoire"
            );
        }

        try {
            ReservationLiberationResponse resultat = restClient.post()
                    .uri("/api/v1/reservations/{id}/liberation", reservationId)
                    .body(Map.of("motif", "ANNULATION"))
                    .retrieve()
                    .body(ReservationLiberationResponse.class);

            if (resultat == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Réponse de libération vide"
                );
            }

            return resultat;

        } catch (RestClientResponseException erreur) {
            if (erreur.getStatusCode().value() == 409) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Conflit lors de la libération du camion"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Erreur du service Camions pendant la libération"
            );

        } catch (ResourceAccessException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Libération incertaine : annulation à reprendre"
            );

        } catch (RestClientException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Réponse de libération invalide"
            );
        }
    }
}