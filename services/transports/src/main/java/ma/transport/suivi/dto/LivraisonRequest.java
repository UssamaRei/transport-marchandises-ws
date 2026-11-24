package ma.transport.suivi.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record LivraisonRequest(
        String receptionnaire,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateReception,

        String commentaire
) {
}
