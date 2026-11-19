package ma.transport.flotte.controller;

import ma.transport.flotte.dto.CamionCreateRequest;
import ma.transport.flotte.dto.CamionPatchRequest;
import ma.transport.flotte.dto.CamionResponse;
import ma.transport.flotte.dto.CamionsPageResponse;
import ma.transport.flotte.exception.CamionNotFoundException;
import ma.transport.flotte.exception.InvalidCamionPatchException;
import ma.transport.flotte.exception.ReservationCapacityConflictException;
import ma.transport.flotte.model.EtatCamion;
import ma.transport.flotte.service.CamionService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mockito.ArgumentCaptor;
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

@WebMvcTest(CamionController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class CamionControllerTest {
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CamionService camionService;

    @Test
    void createReturns201AndLocation() throws Exception {
        when(camionService.create(any(CamionCreateRequest.class))).thenReturn(
                new CamionResponse(
                        "camion-1", "12345-A-1", new BigDecimal("10000"), EtatCamion.EN_SERVICE,
                        false, true, OffsetDateTime.of(2026, 11, 7, 0, 0, 0, 0, ZoneOffset.UTC)));

        mockMvc.perform(post("/api/v1/camions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"immatriculation":"12345-a-1","capaciteKg":10000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/camions/camion-1"))
                .andExpect(jsonPath("$.id").value("camion-1"))
                .andExpect(jsonPath("$.immatriculation").value("12345-A-1"))
                .andExpect(jsonPath("$.capaciteKg").value(10000))
                .andExpect(jsonPath("$.etat").value("EN_SERVICE"))
                .andExpect(jsonPath("$.archive").value(false))
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createReturns422ForMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/camions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createReturns422ForNonPositiveCapacity() throws Exception {
        mockMvc.perform(post("/api/v1/camions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"immatriculation":"12345-A-1","capaciteKg":0}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getReturns404WhenTruckIsMissing() throws Exception {
        when(camionService.getById("unknown")).thenThrow(new CamionNotFoundException("unknown"));

        mockMvc.perform(get("/api/v1/camions/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void listPassesFilterAndPaginationToService() throws Exception {
        when(camionService.list(1, 5, true)).thenReturn(new CamionsPageResponse(
                List.of(new CamionResponse(
                        "camion-1", "12345-A-1", new BigDecimal("10000"), EtatCamion.EN_SERVICE,
                        false, true, OffsetDateTime.of(2026, 11, 7, 0, 0, 0, 0, ZoneOffset.UTC))),
                1, 5, 6, 2));

        mockMvc.perform(get("/api/v1/camions")
                        .param("page", "1")
                        .param("size", "5")
                        .param("disponible", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].disponible").value(true))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void patchReturnsUpdatedTruck() throws Exception {
        when(camionService.update(any(String.class), any(CamionPatchRequest.class))).thenReturn(
                new CamionResponse(
                        "camion-1", "NEW-PLATE", new BigDecimal("10000"), EtatCamion.EN_SERVICE,
                        false, true, OffsetDateTime.of(2026, 11, 7, 0, 0, 0, 0, ZoneOffset.UTC)));

        mockMvc.perform(patch("/api/v1/camions/camion-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"immatriculation":" new-plate "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("camion-1"))
                .andExpect(jsonPath("$.immatriculation").value("NEW-PLATE"))
                .andExpect(jsonPath("$.capaciteKg").value(10000));
    }

    @Test
    void patchRejectsExplicitNullField() throws Exception {
        when(camionService.update(any(String.class), any(CamionPatchRequest.class)))
                .thenThrow(new InvalidCamionPatchException("Valeur nulle interdite."));

        mockMvc.perform(patch("/api/v1/camions/camion-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"capaciteKg":null}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        ArgumentCaptor<CamionPatchRequest> requestCaptor = ArgumentCaptor.forClass(CamionPatchRequest.class);
        verify(camionService).update(org.mockito.ArgumentMatchers.eq("camion-1"), requestCaptor.capture());
        assertTrue(requestCaptor.getValue().capaciteKg().isNull());
    }

    @Test
    void patchRejectsCapacityConflict() throws Exception {
        when(camionService.update(any(String.class), any(CamionPatchRequest.class)))
                .thenThrow(new ReservationCapacityConflictException());

        mockMvc.perform(patch("/api/v1/camions/camion-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"capaciteKg":7000}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_CAPACITY_CONFLICT"));
    }
}
