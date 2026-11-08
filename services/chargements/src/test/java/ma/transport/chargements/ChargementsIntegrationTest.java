package ma.transport.chargements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChargementsIntegrationTest {

    private static final String BASE = "/api/v1/chargements";
    private static final HttpServer CAMIONS = startCamions();
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static volatile String camionResponse;
    private static volatile int camionStatus;
    private static volatile CountDownLatch requestEntered;
    private static volatile CountDownLatch responseAllowed;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private JdbcTemplate jdbc;

    private String camionId;
    private final List<String> createdIds = new ArrayList<>();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("camions.service.url", () -> "http://127.0.0.1:" + CAMIONS.getAddress().getPort());
    }

    private static HttpServer startCamions() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                CALLS.incrementAndGet();
                if (requestEntered != null) {
                    requestEntered.countDown();
                    try {
                        if (!responseAllowed.await(2, TimeUnit.SECONDS)) {
                            throw new IOException("Test response was not released.");
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IOException(exception);
                    }
                }
                String expectedPath = "/api/v1/camions/";
                int status = exchange.getRequestURI().getPath().startsWith(expectedPath)
                        && exchange.getRequestMethod().equals("GET") ? camionStatus : 404;
                byte[] response = camionResponse.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @BeforeEach
    void prepareCamion() {
        camionId = UUID.randomUUID().toString();
        // Ce camion est seulement une fixture pour la cle etrangere de la base partagee.
        // Le service doit toujours lire sa disponibilite par HTTP.
        jdbc.update("INSERT INTO camion (id, immatriculation, capacite_kg) VALUES (?, ?, ?)",
                camionId, "TEST-" + camionId.substring(0, 20), 10000);
        camionStatus = 200;
        camionResponse = camionJson(10000, "EN_SERVICE", false, true);
        CALLS.set(0);
        requestEntered = null;
        responseAllowed = null;
    }

    @AfterEach
    void cleanFixtures() {
        if (responseAllowed != null) {
            responseAllowed.countDown();
        }
        requestEntered = null;
        responseAllowed = null;
        for (String id : createdIds) {
            jdbc.update("DELETE FROM chargement WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM camion WHERE id = ?", camionId);
    }

    @AfterAll
    static void stopCamions() {
        CAMIONS.stop(0);
    }

    @Test
    void createsEmptyDraftAndExposesLocation() throws Exception {
        String id = create("{}");
        mvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("BROUILLON"))
                .andExpect(jsonPath("$.poidsTotalKg").value(0))
                .andExpect(jsonPath("$.lignes").isEmpty());
        assertThat(CALLS.get()).isZero();
    }

    @Test
    void recalculatesWeightAfterAddingUpdatingAndDeletingLines() throws Exception {
        String id = createComplete();
        JsonNode first = addLigne(id, "{\"designation\":\" Colis \",\"quantite\":3,\"poidsUnitaireKg\":0.125}");
        String ligneId = first.get("lignes").get(0).get("id").asText();
        assertThat(first.get("poidsTotalKg").decimalValue()).isEqualByComparingTo("0.375");
        mvc.perform(put(BASE + "/" + id + "/lignes/" + ligneId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"designation\":\"Palettes\",\"quantite\":4,\"poidsUnitaireKg\":2.500}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.poidsTotalKg").value(10));
        mvc.perform(delete(BASE + "/" + id + "/lignes/" + ligneId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.poidsTotalKg").value(0));
    }

    @Test
    void updatesAndDeletesOnlyDrafts() throws Exception {
        String id = create("{}");
        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origine\":\" Agadir \",\"destination\":\"Rabat\",\"destinataireNom\":\"Client\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.origine").value("Agadir"));
        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void validatesThroughActualFeignHttpCallAndReturnsDetailsForBons() throws Exception {
        String id = createReady();
        validate(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDE"))
                .andExpect(jsonPath("$.camionSouhaiteId").value(camionId))
                .andExpect(jsonPath("$.poidsTotalKg").value(100))
                .andExpect(jsonPath("$.lignes[0].designation").value("Cartons"));
        assertThat(CALLS.get()).isEqualTo(1);
        mvc.perform(get(BASE + "/" + id)).andExpect(jsonPath("$.statut").value("VALIDE"));
    }

    @Test
    void repeatedValidationWithSameTruckDoesNotRepeatExternalCall() throws Exception {
        String id = createReady();
        validate(id).andExpect(status().isOk());
        camionStatus = 503;
        validate(id).andExpect(status().isOk());
        assertThat(CALLS.get()).isEqualTo(1);
        mvc.perform(post(BASE + "/" + id + "/validation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"camionId\":\"another-truck\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void validatedLoadingAndAllItsLinesAreImmutable() throws Exception {
        String id = createReady();
        String ligneId = detail(id).get("lignes").get(0).get("id").asText();
        validate(id).andExpect(status().isOk());
        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isConflict());
        mvc.perform(post(BASE + "/" + id + "/lignes").contentType(MediaType.APPLICATION_JSON).content(ligneJson()))
                .andExpect(status().isConflict());
        mvc.perform(put(BASE + "/" + id + "/lignes/" + ligneId).contentType(MediaType.APPLICATION_JSON).content(ligneJson()))
                .andExpect(status().isConflict());
        mvc.perform(delete(BASE + "/" + id + "/lignes/" + ligneId)).andExpect(status().isConflict());
    }

    @Test
    void refusesIncompleteLoadingWithoutCallingCamions() throws Exception {
        String empty = create("{}");
        validate(empty).andExpect(status().isBadRequest());
        String noLines = createComplete();
        validate(noLines).andExpect(status().isBadRequest());
        assertThat(CALLS.get()).isZero();
    }

    @Test
    void refusesOverweightAndRollsBackValidation() throws Exception {
        String id = createReady();
        camionResponse = camionJson(50, "EN_SERVICE", false, true);
        validate(id).andExpect(status().isConflict());
        assertDraft(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"maintenance", "archive", "reserve"})
    void refusesUnavailableTrucks(String situation) throws Exception {
        String id = createReady();
        camionResponse = camionJson(10000, situation.equals("maintenance") ? "MAINTENANCE" : "EN_SERVICE",
                situation.equals("archive"), !situation.equals("reserve"));
        validate(id).andExpect(status().isConflict());
        assertDraft(id);
    }

    @Test
    void mapsTruckNotFoundAndServerFailureWithoutChangingDraft() throws Exception {
        String id = createReady();
        camionStatus = 404;
        validate(id).andExpect(status().isBadRequest());
        camionStatus = 500;
        validate(id).andExpect(status().isServiceUnavailable());
        assertDraft(id);
    }

    @Test
    void refusesIncompleteAndMalformedTruckResponses() throws Exception {
        String id = createReady();
        camionResponse = "{\"capaciteKg\":10000}";
        validate(id).andExpect(status().isBadGateway());
        camionResponse = "not-json";
        validate(id).andExpect(status().isServiceUnavailable());
        assertDraft(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"designation\":\"\",\"quantite\":1,\"poidsUnitaireKg\":1}",
            "{\"designation\":\"X\",\"quantite\":0,\"poidsUnitaireKg\":1}",
            "{\"designation\":\"X\",\"quantite\":1.5,\"poidsUnitaireKg\":1}",
            "{\"designation\":\"X\",\"quantite\":1,\"poidsUnitaireKg\":-1}",
            "{\"designation\":\"X\",\"quantite\":1,\"poidsUnitaireKg\":0.0001}",
            "{\"designation\":\"X\"}"
    })
    void rejectsInvalidLineInput(String json) throws Exception {
        String id = createComplete();
        mvc.perform(post(BASE + "/" + id + "/lignes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        assertThat(detail(id).get("lignes")).isEmpty();
    }

    @Test
    void rollsBackInsertedLineWhenTotalOverflows() throws Exception {
        String id = createReady();
        mvc.perform(post(BASE + "/" + id + "/lignes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"designation\":\"Trop lourd\",\"quantite\":2,\"poidsUnitaireKg\":99999999999.999}"))
                .andExpect(status().isBadRequest());
        JsonNode after = detail(id);
        assertThat(after.get("lignes").size()).isEqualTo(1);
        assertThat(after.get("poidsTotalKg").decimalValue()).isEqualByComparingTo("100");
    }

    @Test
    void cannotModifyLineFromAnotherLoading() throws Exception {
        String first = createReady();
        String second = createReady();
        String ligneId = detail(first).get("lignes").get(0).get("id").asText();
        mvc.perform(put(BASE + "/" + second + "/lignes/" + ligneId)
                        .contentType(MediaType.APPLICATION_JSON).content(ligneJson()))
                .andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/" + second + "/lignes/" + ligneId)).andExpect(status().isNotFound());
        assertThat(detail(first).get("lignes").size()).isEqualTo(1);
    }

    @Test
    void handlesMissingResourcesInvalidJsonAndPagination() throws Exception {
        mvc.perform(get(BASE + "/missing")).andExpect(status().isNotFound());
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"VALIDE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "?limit=101")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "?offset=-1")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "?limit=abc")).andExpect(status().isBadRequest());
        createComplete();
        mvc.perform(get(BASE + "?limit=1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].lignes").doesNotExist());
    }

    @Test
    void concurrentLineEditCannotChangeLoadingAfterValidation() throws Exception {
        String id = createReady();
        String ligneId = detail(id).get("lignes").get(0).get("id").asText();
        requestEntered = new CountDownLatch(1);
        responseAllowed = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var validation = executor.submit(() -> validate(id).andReturn().getResponse().getStatus());
            assertThat(requestEntered.await(2, TimeUnit.SECONDS)).isTrue();
            var modification = executor.submit(() -> mvc.perform(
                    put(BASE + "/" + id + "/lignes/" + ligneId).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"designation\":\"Changed\",\"quantite\":100,\"poidsUnitaireKg\":100}"))
                    .andReturn().getResponse().getStatus());
            responseAllowed.countDown();
            assertThat(validation.get(10, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(modification.get(10, TimeUnit.SECONDS)).isEqualTo(409);
        } finally {
            responseAllowed.countDown();
        }
        assertThat(detail(id).get("poidsTotalKg").decimalValue()).isEqualByComparingTo("100");
    }

    @Test
    void publishesOpenApiDocumentation() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/chargements/{id}/validation'].post").exists());
    }

    private String create(String body) throws Exception {
        MvcResult result = mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        String id = mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
        createdIds.add(id);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(BASE + "/" + id);
        return id;
    }

    private String createComplete() throws Exception {
        return create("{\"origine\":\"Agadir\",\"destination\":\"Casablanca\",\"destinataireNom\":\"Client test\"}");
    }

    private String createReady() throws Exception {
        String id = createComplete();
        addLigne(id, ligneJson());
        return id;
    }

    private String ligneJson() {
        return "{\"designation\":\"Cartons\",\"quantite\":10,\"poidsUnitaireKg\":10}";
    }

    private JsonNode addLigne(String id, String json) throws Exception {
        MvcResult result = mvc.perform(post(BASE + "/" + id + "/lignes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions validate(String id) throws Exception {
        return mvc.perform(post(BASE + "/" + id + "/validation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"camionId\":\"" + camionId + "\"}"));
    }

    private JsonNode detail(String id) throws Exception {
        MvcResult result = mvc.perform(get(BASE + "/" + id)).andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertDraft(String id) throws Exception {
        JsonNode result = detail(id);
        assertThat(result.get("statut").asText()).isEqualTo("BROUILLON");
        assertThat(result.get("camionSouhaiteId").isNull()).isTrue();
    }

    private String camionJson(int capacite, String etat, boolean archive, boolean disponible) {
        return "{\"id\":\"" + camionId + "\",\"capaciteKg\":" + capacite + ",\"etat\":\"" + etat
                + "\",\"archive\":" + archive + ",\"disponible\":" + disponible + "}";
    }
}
