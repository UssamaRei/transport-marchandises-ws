package ma.transport.suivi.service;

import ma.transport.suivi.dto.ReservationResponse;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.repository.TransportRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationConfirmationService {

    private final TransportRepository transportRepository;

    public ReservationConfirmationService(
            TransportRepository transportRepository
    ) {
        this.transportRepository = transportRepository;
    }

    @Transactional
    public TransportResponse confirmer(
            String transportId,
            ReservationResponse reservation
    ) {
        Transport transport = transportRepository.findByIdForUpdate(transportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transport introuvable"
                ));

        verifierReservation(transport, reservation);

        if (transport.getStatut() == StatutTransport.PLANIFIE) {
            if (reservation.id().equals(transport.getReservationId())) {
                // Même confirmation reçue une deuxième fois.
                return TransportResponse.from(transport);
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le transport possède déjà une autre réservation"
            );
        }

        if (transport.getStatut() != StatutTransport.PREPARATION) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le transport n'est plus en préparation"
            );
        }

        transport.confirmerReservation(reservation.id());

        return TransportResponse.from(transport);
    }

    private void verifierReservation(
            Transport transport,
            ReservationResponse reservation
    ) {
        if (reservation == null
                || reservation.id() == null
                || reservation.id().isBlank()
                || reservation.id().length() > 64
                || !transport.getId().equals(reservation.transportId())
                || !transport.getCamionId().equals(reservation.camionId())
                || !"ACTIVE".equals(reservation.statut())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Réservation retournée par Camions invalide ou incohérente"
            );
        }
    }
}