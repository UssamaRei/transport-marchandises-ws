package ma.transport.bons.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PreuveLivraisonRequest(
        @NotBlank @Size(max = 64) String transportId,
        @NotBlank @Size(max = 160) String receptionnaire,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate dateReception,
        @Size(max = 1000) String commentaire,
        @NotBlank @Size(max = 160) @Pattern(regexp = "^livraison:.+$", message = "Doit commencer par livraison:") String operationKey
) { }
