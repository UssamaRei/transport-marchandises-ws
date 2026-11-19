package ma.transport.suivi.dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public final class Dates {
    private Dates() { }

    public static OffsetDateTime normaliser(OffsetDateTime date) {
        return date == null ? null : date.withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
