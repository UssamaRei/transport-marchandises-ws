package ma.transport.suivi.client;

import java.math.BigDecimal;

import ma.transport.suivi.dto.BonDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BonClientTest {

    private MockRestServiceServer serveur;
    private BonClient client;

    @BeforeEach
    void preparer() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://bons.test");

        serveur = MockRestServiceServer.bindTo(builder).build();
        client = new BonClient(builder.build());
    }

    @Test
    void recupererBonEmis() {
        serveur.expect(requestTo("http://bons.test/api/v1/bons/bon-1"))
                .andRespond(withSuccess("""
                        {
                          "id": "bon-1",
                          "statut": "EMIS",
                          "destination": "Casablanca",
                          "poidsTotalKg": 225.000
                        }
                        """, MediaType.APPLICATION_JSON));

        BonDetails bon = client.getBon("bon-1");

        assertEquals("bon-1", bon.id());
        assertEquals("EMIS", bon.statut());
        assertEquals(0, new BigDecimal("225").compareTo(bon.poidsTotalKg()));
        serveur.verify();
    }

    @Test
    void signalerBonIntrouvable() {
        serveur.expect(requestTo("http://bons.test/api/v1/bons/absent"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> client.getBon("absent")
        );

        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatusCode());
        serveur.verify();
    }

    @Test
    void signalerErreurDuServiceBons() {
        serveur.expect(requestTo("http://bons.test/api/v1/bons/bon-1"))
                .andRespond(withServerError());

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> client.getBon("bon-1")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, erreur.getStatusCode());
        serveur.verify();
    }
}