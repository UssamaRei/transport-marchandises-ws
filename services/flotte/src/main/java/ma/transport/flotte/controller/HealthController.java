package ma.transport.flotte.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<?> health(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return ResponseEntity.ok(Map.of("status", "ok"));
            }
            LOGGER.warn("Health check failed: the Neon database connection is invalid");
        } catch (SQLException exception) {
            LOGGER.warn("Health check failed: the Neon database is unavailable", exception);
        }

        String traceId = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "code", "DEPENDENCY_UNAVAILABLE",
                        "message", "La base de donnees Flotte est indisponible.",
                        "details", Map.of(),
                        "correlationId", traceId));
    }
}
