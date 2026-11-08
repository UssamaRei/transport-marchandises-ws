package ma.transport.flotte.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.DecimalNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import ma.transport.flotte.dto.CamionCreateRequest;
import ma.transport.flotte.dto.CamionPatchRequest;
import ma.transport.flotte.dto.CamionResponse;
import ma.transport.flotte.dto.CamionsPageResponse;
import ma.transport.flotte.exception.CamionNotFoundException;
import ma.transport.flotte.exception.DuplicateImmatriculationException;
import ma.transport.flotte.exception.ReservationCapacityConflictException;
import ma.transport.flotte.model.Camion;
import ma.transport.flotte.model.EtatCamion;
import ma.transport.flotte.model.StatutReservation;
import ma.transport.flotte.repository.CamionRepository;
import ma.transport.flotte.repository.ReservationCamionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class   CamionServiceTest {
    @Mock
    private CamionRepository camionRepository;
    @Mock
    private ReservationCamionRepository reservationRepository;

    private CamionService camionService;

    @BeforeEach
    void preparer() {
        camionService = new CamionService(camionRepository, reservationRepository);
    }

    @Test
    void immatriculationNormalisee() {
        // create normalise l'immatriculation avant son enregistrement.
        when(camionRepository.existsByNormalizedImmatriculation("12345 A-1")).thenReturn(false);
        when(camionRepository.saveAndFlush(any(Camion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.existsByCamionIdAndStatut(
                any(String.class), any(StatutReservation.class))).thenReturn(false);

        camionService.create(new CamionCreateRequest(" 12345   a-1 ", new BigDecimal("10000")));

        ArgumentCaptor<Camion> camion = ArgumentCaptor.forClass(Camion.class);
        verify(camionRepository).saveAndFlush(camion.capture());
        assertEquals("12345 A-1", camion.getValue().getImmatriculation());
    }

    @Test
    void immatriculationDupliquee() {
        // create refuse un doublon sans enregistrer un second camion.
        when(camionRepository.existsByNormalizedImmatriculation("12345-A-1")).thenReturn(true);

        assertThrows(DuplicateImmatriculationException.class, () -> camionService.create(
                new CamionCreateRequest(" 12345-a-1 ", new BigDecimal("10000"))));

        verify(camionRepository, never()).saveAndFlush(any(Camion.class));
    }

    @Test
    void camionDisponible() {
        // getById marque disponible un camion en service sans réservation active.
        Camion camion = camion("camion-1", EtatCamion.EN_SERVICE, false);
        when(camionRepository.findById("camion-1")).thenReturn(Optional.of(camion));
        when(reservationRepository.existsByCamionIdAndStatut("camion-1", StatutReservation.ACTIVE))
                .thenReturn(false);

        CamionResponse response = camionService.getById("camion-1");

        assertTrue(response.disponible());
    }

    @Test
    void reservationActive() {
        // getById marque indisponible un camion avec réservation active.
        Camion camion = camion("camion-1", EtatCamion.EN_SERVICE, false);
        when(camionRepository.findById("camion-1")).thenReturn(Optional.of(camion));
        when(reservationRepository.existsByCamionIdAndStatut("camion-1", StatutReservation.ACTIVE))
                .thenReturn(true);

        assertFalse(camionService.getById("camion-1").disponible());
    }

    @Test
    void identifiantInconnu() {
        // getById signale l'absence du camion.
        when(camionRepository.findById("inconnu")).thenReturn(Optional.empty());

        assertThrows(CamionNotFoundException.class, () -> camionService.getById("inconnu"));
    }

    @Test
    void listeFiltree() {
        // list transmet filtre et pagination, puis restitue le total correspondant.
        Camion camion = camion("camion-1", EtatCamion.EN_SERVICE, false);
        PageRequest pagination = PageRequest.of(1, 5);
        when(camionRepository.findByDisponibilite(true, pagination))
                .thenReturn(new PageImpl<>(List.of(camion), pagination, 6));
        when(reservationRepository.existsByCamionIdAndStatut("camion-1", StatutReservation.ACTIVE))
                .thenReturn(false);

        CamionsPageResponse response = camionService.list(1, 5, true);

        verify(camionRepository).findByDisponibilite(true, pagination);
        assertEquals(List.of("camion-1"), response.items().stream().map(CamionResponse::id).toList());
        assertEquals(6, response.totalElements());
        assertEquals(1, response.page());
        assertEquals(5, response.size());
    }

    @Test
    void capaciteModifiee() {
        // update change la capacité demandée et conserve l'immatriculation absente du PATCH.
        Camion camion = camion("camion-1", EtatCamion.EN_SERVICE, false);
        when(camionRepository.findByIdForUpdate("camion-1")).thenReturn(Optional.of(camion));
        when(reservationRepository.sumPoidsByCamionIdAndStatut("camion-1", StatutReservation.ACTIVE))
                .thenReturn(null);
        when(camionRepository.saveAndFlush(camion)).thenReturn(camion);
        when(reservationRepository.existsByCamionIdAndStatut("camion-1", StatutReservation.ACTIVE))
                .thenReturn(false);

        CamionResponse response = camionService.update(
                "camion-1", new CamionPatchRequest(null, DecimalNode.valueOf(new BigDecimal("12000"))));

        assertEquals("12345-A-1", response.immatriculation());
        assertEquals(new BigDecimal("12000"), response.capaciteKg());
    }

    @Test
    void capaciteSousReservation() {
        // update refuse 7000 kg lorsque les réservations actives pèsent 8000 kg.
        Camion camion = camion("camion-1", EtatCamion.EN_SERVICE, false);
        when(camionRepository.findByIdForUpdate("camion-1")).thenReturn(Optional.of(camion));
        when(reservationRepository.sumPoidsByCamionIdAndStatut("camion-1", StatutReservation.ACTIVE))
                .thenReturn(new BigDecimal("8000"));

        assertThrows(ReservationCapacityConflictException.class, () -> camionService.update(
                "camion-1", new CamionPatchRequest(null, DecimalNode.valueOf(new BigDecimal("7000")))));

        verify(camionRepository, never()).saveAndFlush(any(Camion.class));
    }

    private Camion camion(String id, EtatCamion etat, boolean archive) {
        return new Camion(
                id,
                "12345-A-1",
                new BigDecimal("10000"),
                etat,
                archive,
                OffsetDateTime.of(2026, 11, 7, 0, 0, 0, 0, ZoneOffset.UTC));
    }
}
