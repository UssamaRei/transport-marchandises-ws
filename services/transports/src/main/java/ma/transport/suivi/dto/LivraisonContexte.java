package ma.transport.suivi.dto;

public record LivraisonContexte(
        TransportResponse transport,
        PreuveLivraisonRequest preuve
) {
}