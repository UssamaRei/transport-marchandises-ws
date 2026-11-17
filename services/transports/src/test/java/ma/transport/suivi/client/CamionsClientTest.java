package ma.transport.suivi.client;

import java.math.BigDecimal;

import ma.transport.suivi.dto.ReservationRequest;
import ma.transport.suivi.dto.ReservationResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CamionsClientTest {

    private MockRestServiceServer serveur;
    private CamionsClient client;

    @BeforeEach
    void preparer() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://camions.test");

        serveur = MockRestServiceServer.bindTo(builder).build();
        client = new CamionsClient(builder.build());
    }

    @Test
    void creerReservation() {
        serveur.expect(requestTo("http://camions.test/api/v1/reservations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "camionId": "camion-1",
                          "transportId": "transport-1",
                          "poidsKg": 225.000
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id": "reservation-1",
                          "camionId": "camion-1",
                          "transportId": "transport-1",
                          "statut": "ACTIVE"
                        }
                        """, MediaType.APPLICATION_JSON));

        ReservationResponse resultat = client.reserver(
                new ReservationRequest(
                        "camion-1",
                        "transport-1",
                        new BigDecimal("225.000")
                )
        );

        assertEquals("reservation-1", resultat.id());
        assertEquals("ACTIVE", resultat.statut());
        serveur.verify();
    }

    @Test
    void retrouverReservationParTransport() {
        serveur.expect(requestTo(
                        "http://camions.test/api/v1/reservations?transportId=transport-1"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "reservation-1",
                          "camionId": "camion-1",
                          "transportId": "transport-1",
                          "statut": "ACTIVE"
                        }
                        """, MediaType.APPLICATION_JSON));

        ReservationResponse resultat = client.getParTransport("transport-1");

        assertEquals("transport-1", resultat.transportId());
        serveur.verify();
    }

    @Test
    void signalerConflitDeReservation() {
        serveur.expect(requestTo("http://camions.test/api/v1/reservations"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> client.reserver(new ReservationRequest(
                        "camion-1",
                        "transport-1",
                        new BigDecimal("225.000")
                ))
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        serveur.verify();
    }
}