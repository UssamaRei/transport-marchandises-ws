package ma.transport.flotte.service;

import ma.transport.flotte.dto.CamionCreateRequest;
import ma.transport.flotte.dto.CamionPatchRequest;
import ma.transport.flotte.dto.CamionResponse;
import ma.transport.flotte.dto.CamionsPageResponse;
import ma.transport.flotte.exception.CamionNotFoundException;
import ma.transport.flotte.exception.DuplicateImmatriculationException;
import ma.transport.flotte.exception.InvalidCamionPatchException;
import ma.transport.flotte.exception.ReservationCapacityConflictException;
import ma.transport.flotte.model.Camion;
import ma.transport.flotte.model.EtatCamion;
import ma.transport.flotte.model.StatutReservation;
import ma.transport.flotte.repository.CamionRepository;
import ma.transport.flotte.repository.ReservationCamionRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CamionService {
    private final CamionRepository camionRepository;
    private final ReservationCamionRepository reservationRepository;

    public CamionService(CamionRepository camionRepository,
                         ReservationCamionRepository reservationRepository) {
        this.camionRepository = camionRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public CamionResponse create(CamionCreateRequest request) {
        String immatriculation = normalizeImmatriculation(request.immatriculation());
        if (camionRepository.existsByNormalizedImmatriculation(immatriculation)) {
            throw new DuplicateImmatriculationException();
        }

        Camion camion = new Camion(
                UUID.randomUUID().toString(),
                immatriculation,
                request.capaciteKg(),
                EtatCamion.EN_SERVICE,
                false,
                OffsetDateTime.now(ZoneOffset.UTC));
        try {
            camion = camionRepository.saveAndFlush(camion);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateImmatriculationException(exception);
        }
        return toResponse(camion);
    }

    @Transactional(readOnly = true)
    public CamionResponse getById(String id) {
        Camion camion = camionRepository.findById(id)
                .orElseThrow(() -> new CamionNotFoundException(id));
        return toResponse(camion);
    }

    @Transactional(readOnly = true)
    public CamionsPageResponse list(int page, int size, Boolean disponible) {
        Page<Camion> camionPage;
        if (disponible == null) {
            PageRequest pageRequest = PageRequest.of(
                    page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
            camionPage = camionRepository.findAll(pageRequest);
        } else {
            camionPage = camionRepository.findByDisponibilite(disponible, PageRequest.of(page, size));
        }
        List<CamionResponse> items = camionPage.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new CamionsPageResponse(
                items,
                camionPage.getNumber(),
                camionPage.getSize(),
                camionPage.getTotalElements(),
                camionPage.getTotalPages());
    }

    @Transactional
    public CamionResponse update(String id, CamionPatchRequest request) {
        if (request.immatriculation() == null && request.capaciteKg() == null) {
            throw new InvalidCamionPatchException("Au moins un champ modifiable doit etre fourni.");
        }

        Camion camion = camionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new CamionNotFoundException(id));
        String immatriculation = camion.getImmatriculation();
        BigDecimal capaciteKg = camion.getCapaciteKg();

        if (request.immatriculation() != null) {
            if (!request.immatriculation().isTextual()) {
                throw new InvalidCamionPatchException("L'immatriculation doit etre une chaine non nulle.");
            }
            immatriculation = normalizeImmatriculation(request.immatriculation().textValue());
            if (immatriculation.isBlank() || immatriculation.length() > 40) {
                throw new InvalidCamionPatchException("L'immatriculation doit contenir de 1 a 40 caracteres.");
            }
            if (camionRepository.existsByNormalizedImmatriculationAndIdNot(immatriculation, id)) {
                throw new DuplicateImmatriculationException();
            }
        }

        if (request.capaciteKg() != null) {
            if (!request.capaciteKg().isNumber()) {
                throw new InvalidCamionPatchException("La capacite doit etre un nombre strictement positif.");
            }
            capaciteKg = request.capaciteKg().decimalValue();
            if (capaciteKg.signum() <= 0
                    || capaciteKg.precision() - capaciteKg.scale() > 11
                    || capaciteKg.scale() > 3) {
                throw new InvalidCamionPatchException(
                        "La capacite doit etre positive et comporter au maximum 11 chiffres entiers et 3 decimales.");
            }
            BigDecimal poidsReserve = reservationRepository.sumPoidsByCamionIdAndStatut(
                    id, StatutReservation.ACTIVE);
            if (poidsReserve != null && capaciteKg.compareTo(poidsReserve) < 0) {
                throw new ReservationCapacityConflictException();
            }
        }

        camion.update(immatriculation, capaciteKg);
        try {
            camion = camionRepository.saveAndFlush(camion);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateImmatriculationException(exception);
        }
        return toResponse(camion);
    }

    private CamionResponse toResponse(Camion camion) {
        boolean reserve = reservationRepository.existsByCamionIdAndStatut(
                camion.getId(), StatutReservation.ACTIVE);
        boolean disponible = camion.getEtat() == EtatCamion.EN_SERVICE
                && !camion.isArchive()
                && !reserve;
        return new CamionResponse(
                camion.getId(),
                camion.getImmatriculation(),
                camion.getCapaciteKg(),
                camion.getEtat(),
                camion.isArchive(),
                disponible,
                camion.getCreatedAt());
    }

    private String normalizeImmatriculation(String immatriculation) {
        return immatriculation.strip().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
