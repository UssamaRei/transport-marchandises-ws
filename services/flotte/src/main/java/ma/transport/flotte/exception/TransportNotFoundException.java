package ma.transport.flotte.exception;

public class TransportNotFoundException extends RuntimeException {
    public TransportNotFoundException() {
        super("Le transport reference n'existe pas.");
    }
}
