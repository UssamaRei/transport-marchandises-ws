package ma.transport.flotte.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import ma.transport.flotte.dto.ReservationCreateRequest;
import ma.transport.flotte.dto.ReservationReleaseRequest;
import ma.transport.flotte.dto.ReservationResponse;
import ma.transport.flotte.exception.CamionNotFoundException;
import ma.transport.flotte.exception.CamionUnavailableException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock
    private CamionRepository camionRepository;
    @Mock
    private ReservationCamionRepository reservationRepository;

    private ReservationService reservationService;

    @BeforeEach
    void preparer() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        reservationService = new ReservationService(camionRepository, reservationRepository, transactionManager);
    }

    @Test
    void poidsLimite() {
        // create accepte exactement la capacité et crée une réservation ACTIVE.
        Camion camion = camion("camion-1", EtatCamion.EN_SERVICE, false);
        when(camionRepository.findByIdForUpdate("camion-1")).thenReturn(Optional.of(camion));
        when(reservationRepository.findByTransportId("transport-1")).thenReturn(Optional.empty());
        when(reservationRepository.existsByCamionIdAndStatut("camion-1", StatutReservation.ACTIVE))
                .thenReturn(false);
        when(reservationRepository.saveAndFlush(any(ReservationCamion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.create(
                new ReservationCreateRequest("camion-1", "transport-1", new BigDecimal("10000")));

        ArgumentCaptor<ReservationCamion> reservation = ArgumentCaptor.forClass(ReservationCamion.class);
        verify(reservationRepository).saveAndFlush(reservation.capture());
        assertEquals(StatutReservation.ACTIVE, response.statut());
        assertEquals(new BigDecimal("10000"), reservation.getValue().getPoidsKg());
    }

    @Test
    void poidsExcessif() {
        // create refuse 10001 kg pour une capacité de 10000 kg sans enregistrer.
        Camion camion = camion("camion-1", EtatCamion.EN_SERVICE, false);
        when(camionRepository.findByIdForUpdate("camion-1")).thenReturn(Optional.of(camion));
        when(reservationRepository.findByTransportId("transport-1")).thenReturn(Optional.empty());
        when(reservationRepository.existsByCamionIdAndStatut("camion-1", StatutReservation.ACTIVE))
                .thenReturn(false);

        assertThrows(ReservationWeightExceededException.class, () -> reservationService.create(
                new ReservationCreateRequest("camion-1", "transport-1", new BigDecimal("10001"))));

        verify(reservationRepository, never()).saveAndFlush(any(ReservationCamion.class));
    }

    @Test
    void camionDejaReserve() {
        // create refuse un camion possédant déjà une réservation active sans enregistrer.
        Camion camion = camion("camion-1", EtatCamion.EN_SERVICE, false);
        when(camionRepository.findByIdForUpdate("camion-1")).thenReturn(Optional.of(camion));
        when(reservationRepository.findByTransportId("transport-2")).thenReturn(Optional.empty());
        when(reservationRepository.existsByCamionIdAndStatut("camion-1", StatutReservation.ACTIVE))
                .thenReturn(true);

        assertThrows(CamionUnavailableException.class, () -> reservationService.create(
                new ReservationCreateRequest("camion-1", "transport-2", new BigDecimal("1000"))));

        verify(reservationRepository, never()).saveAndFlush(any(ReservationCamion.class));
    }

    @Test
    void transportRejoue() {
        // create retourne la réservation existante pour une même demande.
        ReservationCamion existante = reservation("reservation-1", "camion-1", "transport-1", "8000");
        when(camionRepository.findByIdForUpdate("camion-1"))
                .thenReturn(Optional.of(camion("camion-1", EtatCamion.MAINTENANCE, true)));
        when(reservationRepository.findByTransportId("transport-1")).thenReturn(Optional.of(existante));

        ReservationResponse response = reservationService.create(
                new ReservationCreateRequest("camion-1", "transport-1", new BigDecimal("8000")));

        assertEquals("reservation-1", response.id());
        verify(reservationRepository, never()).saveAndFlush(any(ReservationCamion.class));
    }

    @Test
    void idReservationInconnu() {
        // getById signale une réservation inconnue.
        when(reservationRepository.findById("inconnu")).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> reservationService.getById("inconnu"));
    }

    @Test
    void reservationParId() {
        // getById retourne la réservation associée à son ID.
        ReservationCamion existante = reservation("reservation-1", "camion-1", "transport-1", "8000");
        when(reservationRepository.findById("reservation-1")).thenReturn(Optional.of(existante));

        assertEquals("reservation-1", reservationService.getById("reservation-1").id());
    }

    @Test
    void reservationParTransport() {
        // getByTransportId retrouve la réservation du transport.
        ReservationCamion existante = reservation("reservation-1", "camion-1", "transport-1", "8000");
        when(reservationRepository.findByTransportId("transport-1")).thenReturn(Optional.of(existante));

        assertEquals("reservation-1", reservationService.getByTransportId("transport-1").id());
    }

    @Test
    void transportSansReservation() {
        // getByTransportId signale l'absence de réservation pour ce transport.
        when(reservationRepository.findByTransportId("transport-absent")).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class,
                () -> reservationService.getByTransportId("transport-absent"));
    }

    @Test
    void liberationAnnulation() {
        // release libère la réservation et renseigne motif et date.
        ReservationCamion existante = reservation("reservation-1", "camion-1", "transport-1", "8000");
        preparerLiberation(existante);
        when(reservationRepository.saveAndFlush(existante)).thenReturn(existante);

        ReservationResponse response = reservationService.release(
                "reservation-1", new ReservationReleaseRequest("ANNULATION"));

        assertEquals(StatutReservation.LIBEREE, response.statut());
        assertEquals("ANNULATION", response.motifLiberation());
        assertNotNull(response.releasedAt());
    }

    @Test
    void liberationRejouee() {
        // release avec le même motif conserve la date initiale.
        ReservationCamion existante = reservation("reservation-1", "camion-1", "transport-1", "8000");
        preparerLiberation(existante);
        OffsetDateTime dateInitiale = OffsetDateTime.of(2026, 11, 7, 1, 0, 0, 0, ZoneOffset.UTC);
        existante.release("ANNULATION", dateInitiale);

        ReservationResponse response = reservationService.release(
                "reservation-1", new ReservationReleaseRequest("ANNULATION"));

        assertEquals(dateInitiale, response.releasedAt());
        verify(reservationRepository, never()).saveAndFlush(existante);
    }

    @Test
    void liberationMotifDifferent() {
        // release avec un motif différent d'une libération antérieure crée un conflit.
        ReservationCamion existante = reservation("reservation-1", "camion-1", "transport-1", "8000");
        preparerLiberation(existante);
        existante.release("ANNULATION", OffsetDateTime.now(ZoneOffset.UTC));

        assertThrows(ReservationConflictException.class, () -> reservationService.release(
                "reservation-1", new ReservationReleaseRequest("LIVRAISON")));

        verify(reservationRepository, never()).saveAndFlush(existante);
    }

    @Test
    void transportInexistant() {
        // create traduit la violation de clé étrangère en erreur métier.
        Camion camion = camion("camion-1", EtatCamion.EN_SERVICE, false);
        when(camionRepository.findByIdForUpdate("camion-1")).thenReturn(Optional.of(camion));
        when(reservationRepository.findByTransportId("transport-absent"))
                .thenReturn(Optional.empty(), Optional.empty());
        when(reservationRepository.existsByCamionIdAndStatut("camion-1", StatutReservation.ACTIVE))
                .thenReturn(false);
        when(reservationRepository.saveAndFlush(any(ReservationCamion.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "détail SQL", new SQLException("foreign key", "23503")));

        assertThrows(TransportNotFoundException.class, () -> reservationService.create(
                new ReservationCreateRequest("camion-1", "transport-absent", new BigDecimal("100"))));
    }

    @Test
    void camionInconnu() {
        // create signale un camion absent avant l'enregistrement.
        when(camionRepository.findByIdForUpdate("inconnu")).thenReturn(Optional.empty());

        assertThrows(CamionNotFoundException.class, () -> reservationService.create(
                new ReservationCreateRequest("inconnu", "transport-1", new BigDecimal("100"))));

        verify(reservationRepository, never()).saveAndFlush(any(ReservationCamion.class));
    }

    private void preparerLiberation(ReservationCamion reservation) {
        when(reservationRepository.findCamionIdById("reservation-1")).thenReturn(Optional.of("camion-1"));
        when(camionRepository.findByIdForUpdate("camion-1"))
                .thenReturn(Optional.of(camion("camion-1", EtatCamion.EN_SERVICE, false)));
        when(reservationRepository.findByIdForUpdate("reservation-1")).thenReturn(Optional.of(reservation));
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

    private ReservationCamion reservation(String id, String camionId, String transportId, String poids) {
        return new ReservationCamion(
                id,
                camionId,
                transportId,
                new BigDecimal(poids),
                StatutReservation.ACTIVE,
                null,
                OffsetDateTime.of(2026, 11, 7, 0, 0, 0, 0, ZoneOffset.UTC),
                null);
    }
}
