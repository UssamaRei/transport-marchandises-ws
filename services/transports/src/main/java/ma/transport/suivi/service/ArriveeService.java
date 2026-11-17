package ma.transport.suivi.service;

import java.time.OffsetDateTime;

import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.EvenementSuivi;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.model.TypeEvenement;
import ma.transport.suivi.repository.EvenementSuiviRepository;
import ma.transport.suivi.repository.TransportRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArriveeService {

    private final TransportRepository transportRepository;
    private final EvenementSuiviRepository evenementRepository;

    public ArriveeService(
            TransportRepository transportRepository,
            EvenementSuiviRepository evenementRepository
    ) {
        this.transportRepository = transportRepository;
        this.evenementRepository = evenementRepository;
    }

    @Transactional
    public TransportResponse enregistrer(
            String transportId,
            OffsetDateTime arriveeReelle
    ) {
        if (transportId == null || transportId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identifiant du transport obligatoire"
            );
        }

        if (arriveeReelle == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Date d'arrivée obligatoire"
            );
        }

        Transport transport = transportRepository
                .findByIdForUpdate(transportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transport introuvable"
                ));

        if (transport.getStatut() == StatutTransport.CLOTURE_EN_ATTENTE
                || transport.getStatut() == StatutTransport.LIVRE) {
            if (transport.getArriveeReelle() != null
                    && transport.getArriveeReelle().isEqual(arriveeReelle)) {
                return TransportResponse.from(transport);
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une autre date d'arrivée est déjà enregistrée"
            );
        }

        if (transport.getStatut() != StatutTransport.EN_COURS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le transport doit être en cours"
            );
        }

        if (transport.getDepartReel() == null
                || arriveeReelle.isBefore(transport.getDepartReel())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'arrivée ne peut pas précéder le départ"
            );
        }

        transport.enregistrerArrivee(arriveeReelle);

        evenementRepository.save(new EvenementSuivi(
                transportId,
                TypeEvenement.ARRIVEE,
                "Arrivée déclarée",
                arriveeReelle,
                null,
                null
        ));

        return TransportResponse.from(transport);
    }
}