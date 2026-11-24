package ma.transport.suivi.dto;

public record ReservationResponse(
        String id,
        String camionId,
        String transportId,
        String statut
) {
}
