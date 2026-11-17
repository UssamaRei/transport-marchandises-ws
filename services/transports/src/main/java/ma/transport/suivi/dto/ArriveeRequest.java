package ma.transport.suivi.dto;

import java.time.OffsetDateTime;

public record ArriveeRequest(
        OffsetDateTime arriveeReelle
) {
}