package ma.transport.flotte.exception;

public class ReservationWeightExceededException extends RuntimeException {
    public ReservationWeightExceededException() {
        super("Le poids demande depasse la capacite du camion.");
    }
}
