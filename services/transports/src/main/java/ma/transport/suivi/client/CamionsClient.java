package ma.transport.suivi.client;

import ma.transport.suivi.dto.ReservationRequest;
import ma.transport.suivi.dto.ReservationResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CamionsClient {

    private final RestClient restClient;

    public CamionsClient(@Qualifier("camionsRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ReservationResponse reserver(ReservationRequest demande) {
        try {
            ReservationResponse resultat = restClient.post()
                    .uri("/api/v1/reservations")
                    .body(demande)
                    .retrieve()
                    .body(ReservationResponse.class);

            return verifierReponse(resultat);

        } catch (RestClientResponseException erreur) {
            if (erreur.getStatusCode().value() == 409) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Conflit de réservation côté Camions"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Le service Camions a retourné une erreur"
            );

        } catch (ResourceAccessException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Réponse de réservation incertaine : vérifier par transportId"
            );

        } catch (RestClientException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Réponse invalide du service Camions"
            );
        }
    }

    public ReservationResponse getParTransport(String transportId) {
        if (transportId == null || transportId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Identifiant du transport obligatoire"
            );
        }

        try {
            ReservationResponse resultat = restClient.get()
                    .uri(builder -> builder
                            .path("/api/v1/reservations")
                            .queryParam("transportId", transportId)
                            .build())
                    .retrieve()
                    .body(ReservationResponse.class);

            return verifierReponse(resultat);

        } catch (RestClientResponseException erreur) {
            if (erreur.getStatusCode().value() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Réservation non trouvée par le service Camions"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Le service Camions a retourné une erreur"
            );

        } catch (ResourceAccessException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Le service Camions est inaccessible"
            );

        } catch (RestClientException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Réponse invalide du service Camions"
            );
        }
    }

    private ReservationResponse verifierReponse(ReservationResponse resultat) {
        if (resultat == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Le service Camions a retourné une réponse vide"
            );
        }

        return resultat;
    }
}