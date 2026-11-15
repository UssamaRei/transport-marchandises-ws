package ma.transport.suivi.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ma.transport.suivi.model.StatutTransport;
import ma.transport.suivi.model.SyncEtat;
import ma.transport.suivi.model.Transport;

public record TransportResponse(
    String id,
    String bonId,
    String camionId,
    String reservationId,
    BigDecimal poidsKg,
    String destination,
    OffsetDateTime departPrevu,
    OffsetDateTime departReel,
    OffsetDateTime arriveeReelle,
    StatutTransport statut,
    SyncEtat syncEtat,
    boolean preuveConfirmee,
    boolean liberationConfirmee,
    OffsetDateTime createdAt
) {
    public static TransportResponse from(Transport transport) {
        return new TransportResponse(
            transport.getId(),
            transport.getBonId(),
            transport.getCamionId(),
            transport.getReservationId(),
            transport.getPoidsKg(),
            transport.getDestination(),
            transport.getDepartPrevu(),
            transport.getDepartReel(),
            transport.getArriveeReelle(),
            transport.getStatut(),
            transport.getSyncEtat(),
            transport.isPreuveConfirmee(),
            transport.isLiberationConfirmee(),
            transport.getCreatedAt()
        );
    }
}