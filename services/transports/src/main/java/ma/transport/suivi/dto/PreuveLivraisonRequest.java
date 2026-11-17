package ma.transport.suivi.dto;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

public record PreuveLivraisonRequest(
        String transportId,
        String receptionnaire,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateReception,
        String commentaire,
        String operationKey
) {
}