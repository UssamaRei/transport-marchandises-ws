package ma.transport.suivi.dto;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatesTest {
    @Test
    void requestsUseUtcAndPostgresPrecisionForReplays() {
        OffsetDateTime input = OffsetDateTime.parse("2026-11-19T10:00:00.123456789+01:00");
        OffsetDateTime expected = OffsetDateTime.parse("2026-11-19T09:00:00.123456Z");
        assertEquals(expected, new PlanificationRequest("bon", "camion", input, "op").departPrevu());
        assertEquals(expected, new DepartRequest(input).departReel());
        assertEquals(expected, new ArriveeRequest(input).arriveeReelle());
        assertEquals(expected, new EvenementRequest(null, null, input, null, null).dateEvenement());
    }

    @Test
    void missingDateRemainsMissingForBusinessValidation() {
        assertNull(new DepartRequest(null).departReel());
        assertNull(new PlanificationRequest("bon", "camion", null, "op").departPrevu());
    }
}
