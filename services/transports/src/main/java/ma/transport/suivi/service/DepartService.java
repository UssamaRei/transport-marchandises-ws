package ma.transport.suivi.service;

import ma.transport.suivi.dto.DepartRequest;
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
public class DepartService {

    private final TransportRepository transportRepository;
    private final EvenementSuiviRepository evenementRepository;

    public DepartService(
            TransportRepository transportRepository,
            EvenementSuiviRepository evenementRepository
    ) {
        this.transportRepository = transportRepository;
        this.evenementRepository = evenementRepository;
    }

    @Transactional
    public TransportResponse declarerDepart(
            String transportId,
            DepartRequest request
    ) {
        Transport transport = transportRepository.findByIdForUpdate(transportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transport introuvable"
                ));

        if (request.departReel() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La date de départ est obligatoire"
            );
        }

        if (transport.getStatut() != StatutTransport.PLANIFIE
                || transport.getReservationId() == null
                || transport.getReservationId().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le transport doit être planifié avec une réservation confirmée"
            );
        }

        transport.demarrer(request.departReel());

        EvenementSuivi evenement = new EvenementSuivi(
                transportId,
                TypeEvenement.DEPART,
                "Départ déclaré",
                request.departReel(),
                null,
                null
        );

        evenementRepository.save(evenement);

        return TransportResponse.from(transport);
    }
}