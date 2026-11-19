package ma.transport.suivi.dto;

import java.time.OffsetDateTime;

public record DepartRequest(OffsetDateTime departReel) {
    public DepartRequest {
        departReel = Dates.normaliser(departReel);
    }
}
