package ma.transport.flotte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReservationReleaseRequest(
        @NotBlank @Pattern(regexp = "LIVRAISON|ANNULATION") String motif) {
}
