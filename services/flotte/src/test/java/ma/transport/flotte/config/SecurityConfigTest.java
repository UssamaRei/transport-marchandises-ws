package ma.transport.flotte.config;

import ma.transport.flotte.controller.CamionController;
import ma.transport.flotte.controller.ReservationController;
import ma.transport.flotte.service.CamionService;
import ma.transport.flotte.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CamionController.class, ReservationController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean CamionService camions;
    @MockitoBean ReservationService reservations;

    @Test
    void anonymousCannotReadOrWrite() throws Exception {
        mvc.perform(get("/api/v1/camions")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/reservations")).andExpect(status().isUnauthorized());
        verifyNoInteractions(camions, reservations);
    }

    @Test
    void readerCannotModifyTrucksOrReservations() throws Exception {
        mvc.perform(post("/api/v1/camions").with(jwt().authorities(() -> "ROLE_LECTEUR")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/reservations/r1/liberation").with(jwt().authorities(() -> "ROLE_LECTEUR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(camions, reservations);
    }

    @Test
    void chargementsCanReadTrucksButNotReservations() throws Exception {
        mvc.perform(get("/api/v1/camions/c1").with(jwt().authorities(() -> "ROLE_SERVICE_CHARGEMENTS")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/reservations?transportId=t1")
                        .with(jwt().authorities(() -> "ROLE_SERVICE_CHARGEMENTS")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reservations);
    }

    @Test
    void transportsCannotCreateTrucks() throws Exception {
        mvc.perform(post("/api/v1/camions").with(jwt().authorities(() -> "ROLE_SERVICE_TRANSPORTS")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(camions);
    }
}
