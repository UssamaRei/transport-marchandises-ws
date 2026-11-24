package ma.transport.bons.exception;

import org.springframework.http.HttpStatus;

public class BonException extends RuntimeException {
    private final HttpStatus status;
    public BonException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpStatus getStatus() { return status; }
}
