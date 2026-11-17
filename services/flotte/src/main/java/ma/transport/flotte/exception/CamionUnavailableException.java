package ma.transport.flotte.exception;

public class CamionUnavailableException extends RuntimeException {
    public CamionUnavailableException() {
        super("Le camion n'est pas disponible pour une reservation.");
    }
}
