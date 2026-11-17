package ma.transport.chargements.controller;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import ma.transport.chargements.dto.ApiError;
import ma.transport.chargements.exception.ChargementException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(ChargementException.class)
    public ResponseEntity<ApiError> handleChargement(ChargementException exception) {
        return error(exception.getStatus(), exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                erreurs.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Verifier les champs de la requete.", erreurs);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleParameters(ConstraintViolationException exception) {
        return error(HttpStatus.BAD_REQUEST, "Parametres invalides : limit entre 1 et 100, offset positif ou nul.", Map.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleUnreadable(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Requete invalide : verifier le JSON et les types des champs.", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "Operation incompatible avec les donnees existantes.", Map.of());
    }

    @ExceptionHandler(CannotAcquireLockException.class)
    public ResponseEntity<ApiError> handleLock(CannotAcquireLockException exception) {
        return error(HttpStatus.CONFLICT, "Une autre operation est en cours. Reessayer.", Map.of());
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ApiError> handleDatabaseUnavailable(DataAccessResourceFailureException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "La base de donnees est temporairement indisponible.", Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, Map<String, String> erreurs) {
        ApiError error = new ApiError();
        error.setStatus(status.value());
        error.setMessage(message);
        error.setErreurs(erreurs);
        return ResponseEntity.status(status).body(error);
    }
}
