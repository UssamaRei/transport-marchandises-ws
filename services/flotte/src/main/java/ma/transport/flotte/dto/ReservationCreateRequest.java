package ma.transport.flotte.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReservationCreateRequest(
        @NotBlank @Size(max = 64) String camionId,
        @NotBlank @Size(max = 64) String transportId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 3)
        BigDecimal poidsKg) {
}
