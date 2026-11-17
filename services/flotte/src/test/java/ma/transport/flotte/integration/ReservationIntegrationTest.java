package ma.transport.flotte.integration;

import ma.transport.flotte.dto.ReservationCreateRequest;
import ma.transport.flotte.dto.ReservationResponse;
import ma.transport.flotte.exception.CamionUnavailableException;
import ma.transport.flotte.exception.ReservationConflictException;
import ma.transport.flotte.model.Camion;
import ma.transport.flotte.model.EtatCamion;
import ma.transport.flotte.repository.CamionRepository;
import ma.transport.flotte.repository.ReservationCamionRepository;
import ma.transport.flotte.service.ReservationService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ExtendWith(ReservationTestDatabaseExtension.class)
class ReservationIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
                    .withDatabaseName("flotte_test")
                    .withUsername("flotte_test")
                    .withPassword("flotte_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String databaseUrl = System.getenv("FLOTTE_TEST_DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            validateIsolatedTestDatabaseUrl(databaseUrl);
            registry.add("spring.datasource.url", () -> databaseUrl);
            registry.add("spring.datasource.username",
                    () -> System.getenv().getOrDefault("FLOTTE_TEST_DATABASE_USERNAME", "postgres"));
            registry.add("spring.datasource.password",
                    () -> System.getenv().getOrDefault("FLOTTE_TEST_DATABASE_PASSWORD", ""));
        } else {
            POSTGRES.start();
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "public");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations",
                () -> "classpath:reservation-test-schema.sql");
    }

    private static void validateIsolatedTestDatabaseUrl(String databaseUrl) {
        URI uri = URI.create(databaseUrl.substring("jdbc:".length()));
        String host = uri.getHost();
        if (!"postgresql".equalsIgnoreCase(uri.getScheme())
                || !("localhost".equalsIgnoreCase(host)
                        || "127.0.0.1".equals(host)
                        || "::1".equals(host))
                || !"/flotte_test".equals(uri.getPath())) {
            throw new IllegalArgumentException(
                    "FLOTTE_TEST_DATABASE_URL must target the local flotte_test database, never Neon.");
        }
    }

    @AfterAll
    static void stopPostgresContainer() {
        if (POSTGRES.isRunning()) {
            POSTGRES.stop();
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private ReservationCamionRepository reservationRepository;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.execute("DELETE FROM public.reservation_camion");
        jdbcTemplate.execute("DELETE FROM public.camion");
        jdbcTemplate.execute("DELETE FROM public.transport");
    }

    @Test
    void createsReservationAndSupportsBothLookups() throws Exception {
        String camionId = createTruck("EN_SERVICE", false, "10000");
        String transportId = createTransport();
        String request = reservationJson(camionId, transportId, "8000");

        String response = mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.camionId").value(camionId))
                .andExpect(jsonPath("$.transportId").value(transportId))
                .andExpect(jsonPath("$.poidsKg").value(8000))
                .andExpect(jsonPath("$.statut").value("ACTIVE"))
                .andExpect(jsonPath("$.motifLiberation").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String reservationId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/reservations/{id}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        mockMvc.perform(get("/api/v1/reservations").param("transportId", transportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId));
    }

    @Test
    void rejectsUnavailableTrucksAndExcessiveWeight() throws Exception {
        String maintenanceId = createTruck("MAINTENANCE", false, "10000");
        String archivedId = createTruck("EN_SERVICE", true, "10000");
        String normalId = createTruck("EN_SERVICE", false, "10000");
        String maintenanceTransport = createTransport();
        String archivedTransport = createTransport();
        String heavyTransport = createTransport();

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(maintenanceId, maintenanceTransport, "100")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAMION_UNAVAILABLE"));
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(archivedId, archivedTransport, "100")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAMION_UNAVAILABLE"));
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(normalId, heavyTransport, "10001")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CAPACITY_EXCEEDED"));
    }

    @Test
    void returnsNotFoundForMissingTruckAndReservation() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson("missing-truck", createTransport(), "100")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(get("/api/v1/reservations/missing-reservation"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(get("/api/v1/reservations").param("transportId", "missing-transport"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void rejectsOccupiedTruckAndInvalidTransportWithoutLeakingSql() throws Exception {
        String camionId = createTruck("EN_SERVICE", false, "10000");
        String firstTransport = createTransport();
        String secondTransport = createTransport();
        reservationService.create(new ReservationCreateRequest(
                camionId, firstTransport, new BigDecimal("2000")));

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(camionId, secondTransport, "1000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAMION_UNAVAILABLE"));

        String freeTruckId = createTruck("EN_SERVICE", false, "10000");
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(freeTruckId, "missing-transport", "1000")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRANSPORT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsStringIgnoringCase("foreign key"))));
    }

    @Test
    void repeatedCreationIsIdempotentAndDifferentDataConflicts() throws Exception {
        String camionId = createTruck("EN_SERVICE", false, "10000");
        String transportId = createTransport();
        String request = reservationJson(camionId, transportId, "5000");

        String firstResponse = mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstId = objectMapper.readTree(firstResponse).get("id").asText();
        String secondResponse = mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertEquals(firstId, objectMapper.readTree(secondResponse).get("id").asText());
        assertEquals(1, reservationRepository.count());
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(camionId, transportId, "4000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_CONFLICT"));
    }

    @Test
    void repeatedReleaseKeepsOriginalDateAndDifferentMotifConflicts() throws Exception {
        String camionId = createTruck("EN_SERVICE", false, "10000");
        String transportId = createTransport();
        ReservationResponse created = reservationService.create(
                new ReservationCreateRequest(camionId, transportId, new BigDecimal("5000")));
        String release = """
                {"motif":"LIVRAISON"}
                """;

        String firstResponse = mockMvc.perform(post("/api/v1/reservations/{id}/liberation", created.id())
                        .contentType(MediaType.APPLICATION_JSON).content(release))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("LIBEREE"))
                .andExpect(jsonPath("$.motifLiberation").value("LIVRAISON"))
                .andReturn().getResponse().getContentAsString();
        OffsetDateTime originalReleasedAt =
                OffsetDateTime.parse(objectMapper.readTree(firstResponse).get("releasedAt").asText());

        String repeatedResponse = mockMvc.perform(post("/api/v1/reservations/{id}/liberation", created.id())
                        .contentType(MediaType.APPLICATION_JSON).content(release))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        OffsetDateTime repeatedReleasedAt =
                OffsetDateTime.parse(objectMapper.readTree(repeatedResponse).get("releasedAt").asText());
        assertEquals(originalReleasedAt, repeatedReleasedAt);
        mockMvc.perform(post("/api/v1/reservations/{id}/liberation", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"ANNULATION"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_CONFLICT"));
    }

    @Test
    void concurrentRequestsCannotCreateTwoActiveReservationsForOneTruck() throws Exception {
        String camionId = createTruck("EN_SERVICE", false, "10000");
        String transportOne = createTransport();
        String transportTwo = createTransport();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> reserveConcurrently(
                    camionId, transportOne, ready, start));
            Future<Boolean> second = executor.submit(() -> reserveConcurrently(
                    camionId, transportTwo, ready, start));
            ready.await();
            start.countDown();
            boolean firstSucceeded = first.get();
            boolean secondSucceeded = second.get();

            assertTrue(firstSucceeded ^ secondSucceeded);
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM public.reservation_camion WHERE camion_id = ? AND statut = 'ACTIVE'",
                    Integer.class, camionId));
        }
    }

    private boolean reserveConcurrently(
            String camionId, String transportId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            reservationService.create(new ReservationCreateRequest(
                    camionId, transportId, new BigDecimal("3000")));
            return true;
        } catch (CamionUnavailableException | ReservationConflictException exception) {
            return false;
        }
    }

    private String createTruck(String state, boolean archived, String capacity) {
        String id = UUID.randomUUID().toString();
        camionRepository.saveAndFlush(new Camion(
                id,
                "PLATE-" + id.substring(0, 8),
                new BigDecimal(capacity),
                EtatCamion.valueOf(state),
                archived,
                OffsetDateTime.now(ZoneOffset.UTC)));
        return id;
    }

    private String createTransport() {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO public.transport(id) VALUES (?)", id);
        return id;
    }

    private String reservationJson(String camionId, String transportId, String poidsKg) {
        return """
                {"camionId":"%s","transportId":"%s","poidsKg":%s}
                """.formatted(camionId, transportId, poidsKg);
    }
}
