package ma.transport.flotte.exception;

public class DuplicateImmatriculationException extends RuntimeException {
    public DuplicateImmatriculationException() {
        super("Cette immatriculation existe deja.");
    }

    public DuplicateImmatriculationException(Throwable cause) {
        super("Cette immatriculation existe deja.", cause);
    }
}
