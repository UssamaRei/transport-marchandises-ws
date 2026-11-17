package ma.transport.flotte.exception;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(String id) {
        super("Reservation introuvable : " + id);
    }
}
