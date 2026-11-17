package ma.transport.chargements;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import ma.transport.chargements.model.Chargement;
import ma.transport.chargements.service.ChargementService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.cloud.openfeign.oauth2.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChargementsSecurityTest {

    private static final String BASE = "/api/v1/chargements";
    private static final RSAKey KEY = key();
    private static final HttpServer JWKS = jwks();
    private static final String ISSUER = "http://127.0.0.1:" + JWKS.getAddress().getPort();

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ChargementService service;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:chargements-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> ISSUER + "/certs");
        registry.add("spring.security.oauth2.client.registration.camions.client-secret", () -> "test-secret");
    }

    @AfterAll
    static void stop() {
        JWKS.stop(0);
    }

    @Test
    void rejectsMissingAndMalformedTokens() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(BASE).header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsWrongSignatureIssuerAudienceAndExpiredToken() throws Exception {
        for (String token : List.of(
                token(KEY, "http://untrusted.example", "chargements-api", Instant.now().plusSeconds(300), "ADMIN"),
                token(KEY, ISSUER, "camions-api", Instant.now().plusSeconds(300), "ADMIN"),
                token(KEY, ISSUER, "chargements-api", Instant.now().minusSeconds(120), "ADMIN"),
                token(key(), ISSUER, "chargements-api", Instant.now().plusSeconds(300), "ADMIN"))) {
            mvc.perform(get(BASE).header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }

    @Test
    void readerCanReadButCannotWrite() throws Exception {
        String token = bearer("LECTEUR");
        mvc.perform(get(BASE).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(post(BASE).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAndAgentCanCreate() throws Exception {
        Chargement chargement = new Chargement();
        chargement.setId("new-id");
        when(service.create(any())).thenReturn(chargement);
        for (String role : List.of("ADMIN", "AGENT")) {
            mvc.perform(post(BASE).header("Authorization", bearer(role))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value("new-id"));
        }
    }

    @Test
    void bonsServiceCanReadButCannotCreate() throws Exception {
        String token = bearer("SERVICE_BONS");
        mvc.perform(get(BASE).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(post(BASE).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unrelatedServiceAndUserWithoutRoleAreForbidden() throws Exception {
        for (String role : List.of("SERVICE_TRANSPORTS", "SERVICE_CHARGEMENTS", "UNKNOWN")) {
            mvc.perform(get(BASE).header("Authorization", bearer(role))).andExpect(status().isForbidden());
        }
        mvc.perform(get(BASE).header("Authorization", bearer())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void permitsDocumentationAndDeclaresBearerAuthentication() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists());
    }

    @Test
    void onlyConfiguredFrontendOriginsCanMakePreflightRequests() throws Exception {
        mvc.perform(options(BASE).header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options(BASE).header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }

    private String bearer(String... roles) throws Exception {
        return "Bearer " + token(KEY, ISSUER, "chargements-api", Instant.now().plusSeconds(300), roles);
    }

    private static String token(RSAKey key, String issuer, String audience, Instant expires, String... roles)
            throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID("test-key").type(JOSEObjectType.JWT).build(),
                new JWTClaimsSet.Builder().issuer(issuer).subject("test-user").audience(audience)
                        .issueTime(Date.from(Instant.now().minusSeconds(300)))
                        .expirationTime(Date.from(expires)).claim("roles", List.of(roles)).build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static RSAKey key() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer jwks() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/certs", exchange -> {
                byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
