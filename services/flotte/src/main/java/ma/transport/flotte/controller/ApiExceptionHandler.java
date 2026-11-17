package ma.transport.flotte.controller;

import ma.transport.flotte.dto.ApiError;
import ma.transport.flotte.exception.CamionNotFoundException;
import ma.transport.flotte.exception.CamionUnavailableException;
import ma.transport.flotte.exception.DuplicateImmatriculationException;
import ma.transport.flotte.exception.InvalidCamionPatchException;
import ma.transport.flotte.exception.InvalidReservationException;
import ma.transport.flotte.exception.ReservationCapacityConflictException;
import ma.transport.flotte.exception.ReservationConflictException;
import ma.transport.flotte.exception.ReservationNotFoundException;
import ma.transport.flotte.exception.ReservationWeightExceededException;
import ma.transport.flotte.exception.TransportNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, Object> details = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Valeur invalide" : error.getDefaultMessage(),
                        (first, ignored) -> first));
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR",
                "Les donnees fournies sont invalides.", details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR",
                "Le corps de la requete est absent ou mal forme.", Map.of(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Un parametre de la requete est invalide.", Map.of(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Un parametre de la requete est invalide.", Map.of(), request);
    }

    @ExceptionHandler(CamionNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            CamionNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ApiError> handleReservationNotFound(
            ReservationNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler({CamionUnavailableException.class, ReservationConflictException.class})
    public ResponseEntity<ApiError> handleReservationConflict(
            RuntimeException exception, HttpServletRequest request) {
        String code = exception instanceof CamionUnavailableException
                ? "CAMION_UNAVAILABLE"
                : "RESERVATION_CONFLICT";
        return error(HttpStatus.CONFLICT, code, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler({InvalidReservationException.class, ReservationWeightExceededException.class,
            TransportNotFoundException.class})
    public ResponseEntity<ApiError> handleInvalidReservation(
            RuntimeException exception, HttpServletRequest request) {
        String code;
        if (exception instanceof TransportNotFoundException) {
            code = "TRANSPORT_NOT_FOUND";
        } else if (exception instanceof ReservationWeightExceededException) {
            code = "CAPACITY_EXCEEDED";
        } else {
            code = "VALIDATION_ERROR";
        }
        return error(HttpStatus.UNPROCESSABLE_ENTITY, code, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler({InvalidCamionPatchException.class, ReservationCapacityConflictException.class})
    public ResponseEntity<ApiError> handleCamionConflict(
            RuntimeException exception, HttpServletRequest request) {
        if (exception instanceof ReservationCapacityConflictException) {
            return error(HttpStatus.CONFLICT, "RESERVATION_CAPACITY_CONFLICT",
                    exception.getMessage(), Map.of(), request);
        }
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR",
                exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(DuplicateImmatriculationException.class)
    public ResponseEntity<ApiError> handleDuplicate(
            DuplicateImmatriculationException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_IMMATRICULATION",
                exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDatabaseUnavailable(
            DataAccessException exception, HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE",
                "La base de donnees Flotte est indisponible.", Map.of(), request);
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status, String code, String message, Map<String, Object> details,
            HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return ResponseEntity.status(status)
                .body(new ApiError(code, message, details, correlationId));
    }
}
