package ma.transport.suivi.client;

import java.time.LocalDate;

import ma.transport.suivi.dto.PreuveLivraisonRequest;
import ma.transport.suivi.dto.PreuveLivraisonResponse;

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

class PreuveLivraisonClientTest {

    private MockRestServiceServer serveur;
    private PreuveLivraisonClient client;

    @BeforeEach
    void preparer() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://bons.test");

        serveur = MockRestServiceServer.bindTo(builder).build();
        client = new PreuveLivraisonClient(builder.build());
    }

    @Test
    void transmetLaPreuve() {
        serveur.expect(requestTo(
                        "http://bons.test/api/v1/bons/bon-1/preuve-livraison"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "transportId":"transport-1",
                          "receptionnaire":"Mohamed",
                          "dateReception":"2026-11-17",
                          "commentaire":"Reçu",
                          "operationKey":"preuve-transport-1"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "bonId":"bon-1",
                          "transportId":"transport-1",
                          "receptionnaire":"Mohamed",
                          "dateReception":"2026-11-17",
                          "commentaire":"Reçu",
                          "statut":"LIVRE"
                        }
                        """, MediaType.APPLICATION_JSON));

        PreuveLivraisonRequest demande = new PreuveLivraisonRequest(
                "transport-1",
                "Mohamed",
                LocalDate.parse("2026-11-17"),
                "Reçu",
                "preuve-transport-1"
        );

        PreuveLivraisonResponse resultat =
                client.confirmer("bon-1", demande);

        assertEquals("bon-1", resultat.bonId());
        assertEquals("transport-1", resultat.transportId());
        assertEquals("LIVRE", resultat.statut());
        serveur.verify();
    }

    @Test
    void refuseUnePreuveIncomplete() {
        PreuveLivraisonRequest demande = new PreuveLivraisonRequest(
                "transport-1",
                " ",
                LocalDate.parse("2026-11-17"),
                null,
                "preuve-transport-1"
        );

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> client.confirmer("bon-1", demande)
        );

        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatusCode());
    }

    @Test
    void transmetUnConflitDuServiceBons() {
        serveur.expect(requestTo(
                        "http://bons.test/api/v1/bons/bon-1/preuve-livraison"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        PreuveLivraisonRequest demande = new PreuveLivraisonRequest(
                "transport-1",
                "Mohamed",
                LocalDate.parse("2026-11-17"),
                null,
                "preuve-transport-1"
        );

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> client.confirmer("bon-1", demande)
        );

        assertEquals(HttpStatus.CONFLICT, erreur.getStatusCode());
        serveur.verify();
    }
}