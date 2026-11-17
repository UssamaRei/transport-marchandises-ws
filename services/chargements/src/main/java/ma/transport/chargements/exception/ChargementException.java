package ma.transport.chargements.exception;

import org.springframework.http.HttpStatus;

public class ChargementException extends RuntimeException {

    private final HttpStatus status;

    public ChargementException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
