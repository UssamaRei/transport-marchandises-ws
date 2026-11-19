package ma.transport.flotte.controller;

import ma.transport.flotte.dto.ReservationCreateRequest;
import ma.transport.flotte.dto.ReservationReleaseRequest;
import ma.transport.flotte.dto.ReservationResponse;
import ma.transport.flotte.model.StatutReservation;
import ma.transport.flotte.service.ReservationService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ma.transport.flotte.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(ReservationController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class ReservationControllerTest {
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    void createReturns201AndLocation() throws Exception {
        when(reservationService.create(any(ReservationCreateRequest.class))).thenReturn(reservation());

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"camionId":"camion-1","transportId":"transport-1","poidsKg":8000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/reservations/reservation-1"))
                .andExpect(jsonPath("$.id").value("reservation-1"))
                .andExpect(jsonPath("$.statut").value("ACTIVE"))
                .andExpect(jsonPath("$.releasedAt").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void createRejectsInvalidWeight() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"camionId":"camion-1","transportId":"transport-1","poidsKg":0}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void lookupsReturnReservation() throws Exception {
        when(reservationService.getById("reservation-1")).thenReturn(reservation());
        when(reservationService.getByTransportId("transport-1")).thenReturn(reservation());

        mockMvc.perform(get("/api/v1/reservations/reservation-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transportId").value("transport-1"));
        mockMvc.perform(get("/api/v1/reservations").param("transportId", "transport-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("reservation-1"));
    }

    @Test
    void releaseAcceptsAllowedMotif() throws Exception {
        when(reservationService.release(
                org.mockito.ArgumentMatchers.eq("reservation-1"),
                any(ReservationReleaseRequest.class))).thenReturn(new ReservationResponse(
                        "reservation-1", "camion-1", "transport-1", new BigDecimal("8000"),
                        StatutReservation.LIBEREE, "ANNULATION",
                        OffsetDateTime.parse("2026-11-07T01:00:00Z"),
                        OffsetDateTime.parse("2026-11-07T02:00:00Z")));

        mockMvc.perform(post("/api/v1/reservations/reservation-1/liberation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"ANNULATION"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("LIBEREE"))
                .andExpect(jsonPath("$.motifLiberation").value("ANNULATION"))
                .andExpect(jsonPath("$.releasedAt").value("2026-11-07T02:00:00Z"));
    }

    private ReservationResponse reservation() {
        return new ReservationResponse(
                "reservation-1", "camion-1", "transport-1", new BigDecimal("8000"),
                StatutReservation.ACTIVE, null, OffsetDateTime.of(
                        2026, 11, 7, 1, 0, 0, 0, ZoneOffset.UTC), null);
    }
}
