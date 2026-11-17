package ma.transport.suivi.service;

import ma.transport.suivi.dto.EvenementRequest;
import ma.transport.suivi.dto.EvenementResponse;
import ma.transport.suivi.model.EvenementSuivi;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.model.TypeEvenement;
import ma.transport.suivi.repository.EvenementSuiviRepository;
import ma.transport.suivi.repository.TransportRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EvenementSuiviService {

    private final TransportRepository transportRepository;
    private final EvenementSuiviRepository evenementRepository;

    public EvenementSuiviService(
            TransportRepository transportRepository,
            EvenementSuiviRepository evenementRepository
    ) {
        this.transportRepository = transportRepository;
        this.evenementRepository = evenementRepository;
    }

    @Transactional
    public EvenementResponse ajouter(String transportId, EvenementRequest request) {
        Transport transport = transportRepository.findByIdForUpdate(transportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transport introuvable"
                ));

        if (transport.getStatut() != StatutTransport.EN_COURS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le transport doit être en cours"
            );
        }

        if (request.type() != TypeEvenement.POINTAGE
                && request.type() != TypeEvenement.COMMENTAIRE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le type doit être POINTAGE ou COMMENTAIRE"
            );
        }

        if (request.dateEvenement() == null
                || transport.getDepartReel() == null
                || request.dateEvenement().isBefore(transport.getDepartReel())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La date de l'événement doit être égale ou postérieure au départ"
            );
        }

        try {
            EvenementSuivi evenement = new EvenementSuivi(
                    transportId,
                    request.type(),
                    request.description(),
                    request.dateEvenement(),
                    request.latitude(),
                    request.longitude()
            );

            return EvenementResponse.from(evenementRepository.save(evenement));
        } catch (IllegalArgumentException erreur) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, erreur.getMessage(), erreur
            );
        }
    }

    @Transactional(readOnly = true)
    public Page<EvenementResponse> lister(String transportId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page invalide ou taille hors limites : 1 à 100"
            );
        }

        if (!transportRepository.existsById(transportId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Transport introuvable"
            );
        }

        return evenementRepository
                .findByTransportIdOrderByDateEvenementAscIdAsc(
                        transportId, PageRequest.of(page, size)
                )
                .map(EvenementResponse::from);
    }
}