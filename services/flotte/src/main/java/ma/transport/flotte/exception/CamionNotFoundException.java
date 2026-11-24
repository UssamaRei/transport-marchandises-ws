package ma.transport.flotte.exception;

public class CamionNotFoundException extends RuntimeException {
    public CamionNotFoundException(String id) {
        super("Camion introuvable : " + id);
    }
}
