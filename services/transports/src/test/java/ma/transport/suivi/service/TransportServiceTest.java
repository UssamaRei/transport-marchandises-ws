package ma.transport.suivi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.repository.TransportRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransportServiceTest {

    @Mock
    private TransportRepository repository;

    @InjectMocks
    private TransportService service;

    @Test
    void consulterTransportExistant() {
        OffsetDateTime date = OffsetDateTime.parse("2026-11-15T10:00:00Z");

        Transport transport = new Transport(
                "bon-1",
                "camion-1",
                new BigDecimal("225.000"),
                "Casablanca",
                date,
                "operation-1",
                date
        );

        when(repository.findById(transport.getId()))
                .thenReturn(Optional.of(transport));

        TransportResponse resultat = service.getById(transport.getId());

        assertEquals(transport.getId(), resultat.id());
        assertEquals("bon-1", resultat.bonId());
        assertEquals("Casablanca", resultat.destination());
        verify(repository).findById(transport.getId());
    }

    @Test
    void consulterTransportInexistantRetourne404() {
        when(repository.findById("absent")).thenReturn(Optional.empty());

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.getById("absent")
        );

        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatusCode());
    }

    @Test
    void paginationInvalideRetourne400() {
        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> service.list(null, false, null, null, 0, 101)
        );

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
        verifyNoInteractions(repository);
    }
}