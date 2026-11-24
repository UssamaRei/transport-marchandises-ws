package ma.transport.suivi.dto;

public record ReservationLiberationResponse(
        String id,
        String camionId,
        String transportId,
        String statut,
        String motifLiberation
) {
}
