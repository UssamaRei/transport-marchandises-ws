package ma.transport.suivi.client;

import ma.transport.suivi.dto.BonDetails;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BonClient {

    private final RestClient restClient;

    public BonClient(@Qualifier("bonsRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public BonDetails getBon(String bonId) {
        if (bonId == null || bonId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Identifiant du bon obligatoire"
            );
        }

        try {
            BonDetails bon = restClient.get()
                    .uri("/api/v1/bons/{id}", bonId)
                    .retrieve()
                    .body(BonDetails.class);

            if (bon == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Le service Bons a retourné une réponse vide"
                );
            }

            return bon;

        } catch (RestClientResponseException erreur) {
            if (erreur.getStatusCode().value() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bon introuvable"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Le service Bons a retourné une erreur"
            );

        } catch (ResourceAccessException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Le service Bons est inaccessible"
            );

        } catch (RestClientException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Réponse invalide du service Bons"
            );
        }
    }
}