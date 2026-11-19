package ma.transport.flotte.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import ma.transport.flotte.controller.CamionController;
import ma.transport.flotte.controller.HealthController;
import ma.transport.flotte.controller.ReservationController;
import ma.transport.flotte.service.CamionService;
import ma.transport.flotte.service.ReservationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CamionController.class, ReservationController.class, HealthController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {
    private static final String ISSUER = "http://localhost:8090/realms/transport-ws";
    private static final String AUDIENCE = "camions-api";
    private static final RSAKey KEY = signingKey();
    private static final HttpServer JWKS = startJwks();

    @Autowired MockMvc mvc;
    @MockitoBean CamionService camions;
    @MockitoBean ReservationService reservations;
    @MockitoBean DataSource dataSource;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/certs");
    }

    @AfterAll
    static void stopJwks() {
        JWKS.stop(0);
    }

    @Test
    void anonymousCannotReadOrWrite() throws Exception {
        mvc.perform(get("/api/v1/camions")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/reservations")).andExpect(status().isUnauthorized());
        verifyNoInteractions(camions, reservations);
    }

    @Test
    void rejectsInvalidSignatureIssuerAudienceAndExpiration() throws Exception {
        Instant now = Instant.now();
        for (String token : List.of(
                sign(signingKey(), ISSUER, AUDIENCE, now.plusSeconds(300), "ADMIN"),
                sign(KEY, "https://other.example/realms/transport-ws", AUDIENCE, now.plusSeconds(300), "ADMIN"),
                sign(KEY, ISSUER, "transports-api", now.plusSeconds(300), "ADMIN"),
                sign(KEY, ISSUER, AUDIENCE, now.minusSeconds(120), "ADMIN"),
                sign(KEY, ISSUER, AUDIENCE, null, "ADMIN"))) {
            mvc.perform(get("/api/v1/camions").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(camions);
    }

    @Test
    void healthIsPublic() throws Exception {
        var connection = mock(java.sql.Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        mvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void readerCannotModifyTrucksOrReservations() throws Exception {
        String token = bearer("LECTEUR");
        mvc.perform(post("/api/v1/camions").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/reservations/r1/liberation").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        verifyNoInteractions(camions, reservations);
        mvc.perform(get("/api/v1/reservations/r1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void chargementsCanReadTrucksButNotReservations() throws Exception {
        String token = bearer("SERVICE_CHARGEMENTS");
        mvc.perform(get("/api/v1/camions/c1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/reservations?transportId=t1")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reservations);
    }

    @Test
    void transportsCanReadAndReleaseReservationsButCannotCreateTrucks() throws Exception {
        String token = bearer("SERVICE_TRANSPORTS");
        mvc.perform(post("/api/v1/camions").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        verifyNoInteractions(camions);
        mvc.perform(get("/api/v1/reservations").param("transportId", "t1")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/reservations/r1/liberation").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"LIVRAISON\"}"))
                .andExpect(status().isOk());
    }

    private static String bearer(String role) throws JOSEException {
        return sign(KEY, ISSUER, AUDIENCE, Instant.now().plusSeconds(300), role);
    }

    private static String sign(RSAKey key, String issuer, String audience, Instant expiresAt, String role)
            throws JOSEException {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().issuer(issuer).subject("test-user")
                .audience(audience).claim("roles", List.of(role));
        if (expiresAt != null) {
            claims.expirationTime(Date.from(expiresAt));
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                claims.build());
        jwt.sign(new RSASSASigner(key));
        return "Bearer " + jwt.serialize();
    }

    private static RSAKey signingKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer startJwks() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            byte[] jwks = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            server.createContext("/certs", exchange -> {
                try (exchange) {
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, jwks.length);
                    exchange.getResponseBody().write(jwks);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
