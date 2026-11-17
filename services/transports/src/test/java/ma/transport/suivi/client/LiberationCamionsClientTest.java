package ma.transport.suivi.client;

import ma.transport.suivi.dto.ReservationLiberationResponse;

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

class LiberationCamionsClientTest {

    private MockRestServiceServer serveur;
    private LiberationCamionsClient client;

    @BeforeEach
    void preparer() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://camions.test");

        serveur = MockRestServiceServer.bindTo(builder).build();
        client = new LiberationCamionsClient(builder.build());
    }

    @Test
    void liberePourLivraison() {
        serveur.expect(requestTo(
                        "http://camions.test/api/v1/reservations/res-1/liberation"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"motif":"LIVRAISON"}
                        """))
                .andRespond(withSuccess("""
                        {
                          "id":"res-1",
                          "camionId":"camion-1",
                          "transportId":"transport-1",
                          "statut":"LIBEREE",
                          "motifLiberation":"LIVRAISON"
                        }
                        """, MediaType.APPLICATION_JSON));

        ReservationLiberationResponse resultat =
                client.libererPourLivraison("res-1");

        assertEquals("res-1", resultat.id());
        assertEquals("LIVRAISON", resultat.motifLiberation());
        serveur.verify();
    }

    @Test
    void conserveLeMotifAnnulation() {
        serveur.expect(requestTo(
                        "http://camions.test/api/v1/reservations/res-1/liberation"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"motif":"ANNULATION"}
                        """))
                .andRespond(withSuccess("""
                        {
                          "id":"res-1",
                          "camionId":"camion-1",
                          "transportId":"transport-1",
                          "statut":"LIBEREE",
                          "motifLiberation":"ANNULATION"
                        }
                        """, MediaType.APPLICATION_JSON));

        ReservationLiberationResponse resultat =
                client.libererPourAnnulation("res-1");

        assertEquals("ANNULATION", resultat.motifLiberation());
        serveur.verify();
    }

    @Test
    void refuseIdentifiantVide() {
        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> client.libererPourLivraison(" ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
    }
}