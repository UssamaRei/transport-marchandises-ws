package ma.transport.suivi.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ma.transport.suivi.dto.BonDetails;
import ma.transport.suivi.dto.PlanificationRequest;
import ma.transport.suivi.dto.TransportResponse;
import ma.transport.suivi.model.Transport;
import ma.transport.suivi.service.PlanificationService;
import ma.transport.suivi.service.TransportPreparationFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PlanificationControllerTest {

    @Mock
    private PlanificationService service;

    private MockMvc mvc;
    private Transport transport;

    private static final String DEMANDE_JSON = """
            {
              "bonId": "bon-1",
              "camionId": "camion-1",
              "departPrevu": "2026-11-16T08:00:00Z",
              "operationKey": "operation-1"
            }
            """;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(new PlanificationController(service))
                .build();

        PlanificationRequest demande = new PlanificationRequest(
                "bon-1",
                "camion-1",
                OffsetDateTime.parse("2026-11-16T08:00:00Z"),
                "operation-1"
        );

        BonDetails bon = new BonDetails(
                "bon-1", "EMIS", "Casablanca",
                new BigDecimal("225.000")
        );

        transport = new TransportPreparationFactory()
                .preparer(demande, bon);
        transport.confirmerReservation("reservation-1");
    }

    @Test
    void postRetourneLeTransportPlanifie() throws Exception {
        when(service.planifier(any(PlanificationRequest.class)))
                .thenReturn(TransportResponse.from(transport));

        mvc.perform(post("/api/v1/transports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEMANDE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transport.getId()))
                .andExpect(jsonPath("$.statut").value("PLANIFIE"));

        verify(service).planifier(any(PlanificationRequest.class));
    }

    @Test
    void corpsAbsentEstRefuse() throws Exception {
        mvc.perform(post("/api/v1/transports")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void erreurDuServiceEstTransmise() throws Exception {
        when(service.planifier(any(PlanificationRequest.class)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Service Camions inaccessible"
                ));

        mvc.perform(post("/api/v1/transports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEMANDE_JSON))
                .andExpect(status().isServiceUnavailable());
    }
}