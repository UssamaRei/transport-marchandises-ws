package ma.transport.flotte.service;

import ma.transport.flotte.dto.ReservationCreateRequest;
import ma.transport.flotte.dto.ReservationReleaseRequest;
import ma.transport.flotte.dto.ReservationResponse;
import ma.transport.flotte.exception.CamionNotFoundException;
import ma.transport.flotte.exception.CamionUnavailableException;
import ma.transport.flotte.exception.InvalidReservationException;
import ma.transport.flotte.exception.ReservationConflictException;
import ma.transport.flotte.exception.ReservationNotFoundException;
import ma.transport.flotte.exception.ReservationWeightExceededException;
import ma.transport.flotte.exception.TransportNotFoundException;
import ma.transport.flotte.model.Camion;
import ma.transport.flotte.model.EtatCamion;
import ma.transport.flotte.model.ReservationCamion;
import ma.transport.flotte.model.StatutReservation;
import ma.transport.flotte.repository.CamionRepository;
import ma.transport.flotte.repository.ReservationCamionRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReservationService {
    private final CamionRepository camionRepository;
    private final ReservationCamionRepository reservationRepository;
    private final TransactionTemplate transactionTemplate;

    public ReservationService(
            CamionRepository camionRepository,
            ReservationCamionRepository reservationRepository,
            PlatformTransactionManager transactionManager) {
        this.camionRepository = camionRepository;
        this.reservationRepository = reservationRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ReservationResponse create(ReservationCreateRequest request) {
        if (request.poidsKg() == null || request.poidsKg().signum() <= 0
                || request.poidsKg().precision() - request.poidsKg().scale() > 11
                || request.poidsKg().scale() > 3) {
            throw new InvalidReservationException("Le poids doit etre positif et comporter au maximum 11 chiffres entiers et 3 decimales.");
        }

        try {
            return transactionTemplate.execute(status -> createInTransaction(request));
        } catch (DataIntegrityViolationException exception) {
            Optional<ReservationCamion> existing = reservationRepository.findByTransportId(request.transportId());
            if (existing.isPresent()) {
                return resolveExisting(existing.get(), request);
            }
            if (hasSqlState(exception, "23503")) {
                throw new TransportNotFoundException();
            }
            if (hasSqlState(exception, "23505")) {
                throw new ReservationConflictException("Le camion possede deja une reservation active.");
            }
            throw exception;
        }
    }

    private ReservationResponse createInTransaction(ReservationCreateRequest request) {
        Camion camion = camionRepository.findByIdForUpdate(request.camionId())
                .orElseThrow(() -> new CamionNotFoundException(request.camionId()));

        Optional<ReservationCamion> existing = reservationRepository.findByTransportId(request.transportId());
        if (existing.isPresent()) {
            return resolveExisting(existing.get(), request);
        }

        if (camion.getEtat() != EtatCamion.EN_SERVICE || camion.isArchive()) {
            throw new CamionUnavailableException();
        }
        if (reservationRepository.existsByCamionIdAndStatut(camion.getId(), StatutReservation.ACTIVE)) {
            throw new CamionUnavailableException();
        }
        if (request.poidsKg().compareTo(camion.getCapaciteKg()) > 0) {
            throw new ReservationWeightExceededException();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        ReservationCamion reservation = new ReservationCamion(
                UUID.randomUUID().toString(),
                camion.getId(),
                request.transportId(),
                request.poidsKg(),
                StatutReservation.ACTIVE,
                null,
                now,
                null);
        return toResponse(reservationRepository.saveAndFlush(reservation));
    }

    public ReservationResponse getById(String id) {
        return reservationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public ReservationResponse getByTransportId(String transportId) {
        return reservationRepository.findByTransportId(transportId)
                .map(this::toResponse)
                .orElseThrow(() -> new ReservationNotFoundException(transportId));
    }

    public ReservationResponse release(String id, ReservationReleaseRequest request) {
        return transactionTemplate.execute(status -> {
            String camionId = reservationRepository.findCamionIdById(id)
                    .orElseThrow(() -> new ReservationNotFoundException(id));
            camionRepository.findByIdForUpdate(camionId)
                    .orElseThrow(() -> new CamionNotFoundException(camionId));
            ReservationCamion reservation = reservationRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new ReservationNotFoundException(id));

            if (reservation.getStatut() == StatutReservation.LIBEREE) {
                if (!request.motif().equals(reservation.getMotifLiberation())) {
                    throw new ReservationConflictException(
                            "La reservation a deja ete liberee avec un motif different.");
                }
                return toResponse(reservation);
            }

            reservation.release(request.motif(), OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS));
            return toResponse(reservationRepository.saveAndFlush(reservation));
        });
    }

    private ReservationResponse resolveExisting(ReservationCamion reservation, ReservationCreateRequest request) {
        if (reservation.getCamionId().equals(request.camionId())
                && reservation.getPoidsKg().compareTo(request.poidsKg()) == 0) {
            return toResponse(reservation);
        }
        throw new ReservationConflictException(
                "Ce transport possede deja une reservation avec des donnees differentes.");
    }

    private ReservationResponse toResponse(ReservationCamion reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getCamionId(),
                reservation.getTransportId(),
                reservation.getPoidsKg(),
                reservation.getStatut(),
                reservation.getMotifLiberation(),
                reservation.getCreatedAt(),
                reservation.getReleasedAt());
    }

    private boolean hasSqlState(Throwable exception, String sqlState) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                if (sqlState.equals(sqlException.getSQLState())) {
                    return true;
                }
                SQLException next = sqlException.getNextException();
                while (next != null) {
                    if (sqlState.equals(next.getSQLState())) {
                        return true;
                    }
                    next = next.getNextException();
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
