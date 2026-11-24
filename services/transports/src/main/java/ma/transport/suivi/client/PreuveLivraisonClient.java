package ma.transport.suivi.client;

import ma.transport.suivi.dto.PreuveLivraisonRequest;
import ma.transport.suivi.dto.PreuveLivraisonResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PreuveLivraisonClient {

    private final RestClient restClient;

    public PreuveLivraisonClient(
            @Qualifier("bonsRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public PreuveLivraisonResponse confirmer(
            String bonId,
            PreuveLivraisonRequest demande
    ) {
        valider(bonId, demande);

        try {
            PreuveLivraisonResponse resultat = restClient.post()
                    .uri("/api/v1/bons/{id}/preuve-livraison", bonId)
                    .body(demande)
                    .retrieve()
                    .body(PreuveLivraisonResponse.class);

            if (resultat == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Réponse de preuve de livraison vide"
                );
            }

            return resultat;

        } catch (RestClientResponseException erreur) {
            if (erreur.getStatusCode().value() == 409) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Conflit lors de la confirmation de la preuve"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Erreur du service Bons pendant la confirmation"
            );

        } catch (ResourceAccessException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Confirmation incertaine : opération à reprendre"
            );

        } catch (RestClientException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Réponse du service Bons invalide"
            );
        }
    }

    private void valider(String bonId, PreuveLivraisonRequest demande) {
        if (bonId == null || bonId.isBlank() || bonId.length() > 64
                || demande == null
                || demande.transportId() == null
                || demande.transportId().isBlank()
                || demande.transportId().length() > 64
                || demande.receptionnaire() == null
                || demande.receptionnaire().isBlank()
                || demande.receptionnaire().length() > 160
                || demande.dateReception() == null
                || demande.operationKey() == null
                || demande.operationKey().isBlank()
                || demande.operationKey().length() > 160
                || (demande.commentaire() != null
                && demande.commentaire().length() > 1000)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Données de preuve de livraison invalides"
            );
        }
    }
}
