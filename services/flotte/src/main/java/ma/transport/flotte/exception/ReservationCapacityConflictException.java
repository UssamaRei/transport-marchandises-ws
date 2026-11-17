package ma.transport.flotte.exception;

public class ReservationCapacityConflictException extends RuntimeException {
    public ReservationCapacityConflictException() {
        super("La capacite ne peut pas etre inferieure au poids reserve.");
    }
}
