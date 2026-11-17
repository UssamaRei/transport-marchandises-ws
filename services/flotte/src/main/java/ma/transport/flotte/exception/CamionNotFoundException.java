package ma.transport.flotte.exception;

import ma.transport.flotte.model.Camion;
public class CamionNotFoundException extends RuntimeException {
    public CamionNotFoundException(String id) {
        super("Camion introuvable : " + id);
    }
}
