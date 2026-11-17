package ma.transport.flotte.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HealthControllerTest {

    @Test
    void returnsOkWhenDatabaseConnectionIsValid() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        ResponseEntity<?> response = new HealthController(dataSource).health(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("status", "ok"), response.getBody());
    }

    @Test
    void returnsServiceUnavailableWhenDatabaseConnectionFails() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Database unavailable"));

        ResponseEntity<?> response = new HealthController(dataSource).health("trace-123");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("DEPENDENCY_UNAVAILABLE", body.get("code"));
        assertEquals("trace-123", body.get("correlationId"));
        assertEquals(Map.of(), body.get("details"));
    }
}
