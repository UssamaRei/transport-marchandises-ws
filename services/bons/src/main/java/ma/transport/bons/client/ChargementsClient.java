package ma.transport.bons.client;

import ma.transport.bons.dto.ChargementDetails;
import ma.transport.bons.exception.BonException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ChargementsClient {
    private final RestClient restClient;
    public ChargementsClient(@Qualifier("chargementsRestClient") RestClient restClient) { this.restClient = restClient; }

    public ChargementDetails getChargement(String id) {
        try {
            ChargementDetails detail = restClient.get().uri("/api/v1/chargements/{id}", id)
                    .retrieve().body(ChargementDetails.class);
            if (detail == null) {
                throw new BonException(HttpStatus.BAD_GATEWAY, "Réponse vide du service Chargements.");
            }
            return detail;
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 404) {
                throw new BonException(HttpStatus.NOT_FOUND, "Chargement introuvable.");
            }
            throw new BonException(HttpStatus.BAD_GATEWAY, "Le service Chargements a retourné une erreur.");
        } catch (ResourceAccessException error) {
            throw new BonException(HttpStatus.SERVICE_UNAVAILABLE, "Le service Chargements est inaccessible.");
        } catch (RestClientException error) {
            throw new BonException(HttpStatus.BAD_GATEWAY, "Réponse invalide du service Chargements.");
        }
    }
}
