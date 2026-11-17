package ma.transport.suivi.controller;

import java.time.OffsetDateTime;

import ma.transport.suivi.service.ArriveeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArriveeControllerTest {

    private ArriveeService service;
    private MockMvc mockMvc;

    @BeforeEach
    void preparer() {
        service = mock(ArriveeService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ArriveeController(service))
                .build();
    }

    @Test
    void transmetLaDateAuService() throws Exception {
        mockMvc.perform(post("/api/v1/transports/transport-1/arrivee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"arriveeReelle":"2026-11-17T11:00:00Z"}
                        """))
                .andExpect(status().isOk());

        verify(service).enregistrer(
                "transport-1",
                OffsetDateTime.parse("2026-11-17T11:00:00Z")
        );
    }

    @Test
    void refuseUneRequeteSansCorps() throws Exception {
        mockMvc.perform(post("/api/v1/transports/transport-1/arrivee")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void refuseUneDateInvalide() throws Exception {
        mockMvc.perform(post("/api/v1/transports/transport-1/arrivee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"arriveeReelle":"date-invalide"}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}