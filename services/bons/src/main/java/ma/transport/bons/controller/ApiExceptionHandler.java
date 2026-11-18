package ma.transport.bons.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import ma.transport.bons.exception.BonException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(int status, String message, Map<String, String> erreurs) { }

    @ExceptionHandler(BonException.class)
    ResponseEntity<ApiError> bon(BonException e) { return error(e.getStatus(), e.getMessage(), Map.of()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(item -> errors.putIfAbsent(item.getField(), item.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Vérifier les champs de la requête.", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "Requête invalide : vérifier le JSON et les types des champs.", Map.of());
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    ResponseEntity<ApiError> unavailable(DataAccessResourceFailureException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "La base de données est temporairement indisponible.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> internal(Exception e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue.", Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message, errors));
    }
}
