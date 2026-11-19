package ma.transport.suivi.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import ma.transport.suivi.dto.BonDetails;
import ma.transport.suivi.dto.PlanificationRequest;
import ma.transport.suivi.model.Transport;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransportPreparationFactory {

    public Transport preparer(PlanificationRequest demande, BonDetails bon) {
        if (demande == null
                || invalide(demande.bonId(), 64)
                || invalide(demande.camionId(), 64)
                || invalide(demande.operationKey(), 160)
                || demande.departPrevu() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bon, camion, départ prévu et clé d'opération sont obligatoires"
            );
        }

        if (bon == null
                || invalide(bon.id(), 64)
                || invalide(bon.destination(), 500)
                || bon.poidsTotalKg() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Réponse du service Bons incomplète"
            );
        }

        if (!demande.bonId().equals(bon.id())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Le bon retourné ne correspond pas au bon demandé"
            );
        }

        if (!"EMIS".equals(bon.statut())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le bon doit être émis pour planifier un transport"
            );
        }

        try {
            return new Transport(
                    bon.id(),
                    demande.camionId(),
                    bon.poidsTotalKg(),
                    bon.destination(),
                    demande.departPrevu(),
                    demande.operationKey(),
                    OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
            );
        } catch (IllegalArgumentException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Données du bon invalides : " + erreur.getMessage(),
                    erreur
            );
        }
    }

    private boolean invalide(String valeur, int maximum) {
        return valeur == null || valeur.isBlank() || valeur.length() > maximum;
    }
}
