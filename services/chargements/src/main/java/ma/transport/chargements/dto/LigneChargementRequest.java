package ma.transport.chargements.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LigneChargementRequest {

    @NotBlank
    @Size(max = 240)
    private String designation;

    @NotNull
    @Min(1)
    private Integer quantite;

    @NotNull
    @DecimalMin("0.001")
    @Digits(integer = 11, fraction = 3)
    private BigDecimal poidsUnitaireKg;

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPoidsUnitaireKg() {
        return poidsUnitaireKg;
    }

    public void setPoidsUnitaireKg(BigDecimal poidsUnitaireKg) {
        this.poidsUnitaireKg = poidsUnitaireKg;
    }

}
