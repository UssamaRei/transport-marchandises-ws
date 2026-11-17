package ma.transport.chargements.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ValidationChargementRequest {

    @NotBlank
    @Size(max = 64)
    private String camionId;

    public String getCamionId() {
        return camionId;
    }

    public void setCamionId(String camionId) {
        this.camionId = camionId;
    }

}
