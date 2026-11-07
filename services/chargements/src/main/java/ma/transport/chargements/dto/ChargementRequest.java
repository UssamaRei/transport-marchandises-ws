package ma.transport.chargements.dto;

import jakarta.validation.constraints.Size;

public class ChargementRequest {

    @Size(max = 500)
    private String origine;

    @Size(max = 500)
    private String destination;

    @Size(max = 160)
    private String destinataireNom;

    @Size(max = 40)
    private String destinataireTelephone;

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDestinataireNom() {
        return destinataireNom;
    }

    public void setDestinataireNom(String destinataireNom) {
        this.destinataireNom = destinataireNom;
    }

    public String getDestinataireTelephone() {
        return destinataireTelephone;
    }

    public void setDestinataireTelephone(String destinataireTelephone) {
        this.destinataireTelephone = destinataireTelephone;
    }

}
