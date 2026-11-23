package ma.transport.bons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreerBonRequest(
        @NotBlank @Size(max = 64) String chargementId
) { }
