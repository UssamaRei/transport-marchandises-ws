package ma.transport.suivi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvenementSuiviTest {

    private static final OffsetDateTime DATE =
            OffsetDateTime.parse("2026-11-15T10:00:00Z");

    @Test
    void creerUnPointageValide() {
        EvenementSuivi evenement = new EvenementSuivi(
                "transport-1",
                TypeEvenement.POINTAGE,
                "Passage à Marrakech",
                DATE,
                null,
                null
        );

        assertNotNull(evenement.getId());
        assertEquals("transport-1", evenement.getTransportId());
        assertEquals(TypeEvenement.POINTAGE, evenement.getType());
        assertEquals(DATE, evenement.getDateEvenement());
    }

    @Test
    void refuserUneLatitudeInvalide() {
        assertThrows(IllegalArgumentException.class, () ->
                new EvenementSuivi(
                        "transport-1",
                        TypeEvenement.POINTAGE,
                        "Position invalide",
                        DATE,
                        new BigDecimal("91"),
                        null
                )
        );
    }
}