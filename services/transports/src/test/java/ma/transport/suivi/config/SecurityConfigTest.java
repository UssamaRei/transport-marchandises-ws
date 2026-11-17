package ma.transport.suivi.config;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SecurityConfigTest.TestController.class,
        properties = "TRANSPORTS_CLIENT_SECRET=secret-fictif-pour-test"
)
@Import({SecurityConfig.class, SecurityConfigTest.TestController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @RestController
    static class TestController {

        @GetMapping("/api/v1/transports/test")
        String lire() {
            return "ok";
        }

        @PostMapping("/api/v1/transports/test")
        String modifier() {
            return "ok";
        }

        @GetMapping("/api/v1/transports")
        String lister() {
            return "ok";
        }
    }

    private Jwt token(String valeur, String role) {
        return Jwt.withTokenValue(valeur)
                .header("alg", "RS256")
                .subject("utilisateur-test")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("roles", List.of(role))
                .build();
    }

    @Test
    void sansTokenAccesRefuse() throws Exception {
        mvc.perform(get("/api/v1/transports/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalideAccesRefuse() throws Exception {
        when(jwtDecoder.decode("invalide"))
                .thenThrow(new BadJwtException("Token invalide"));

        mvc.perform(get("/api/v1/transports/test")
                        .header("Authorization", "Bearer invalide"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lecteurPeutLireMaisPasModifier() throws Exception {
        when(jwtDecoder.decode("lecteur"))
                .thenReturn(token("lecteur", "LECTEUR"));

        mvc.perform(get("/api/v1/transports/test")
                        .header("Authorization", "Bearer lecteur"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/transports/test")
                        .header("Authorization", "Bearer lecteur")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void responsablePeutModifier() throws Exception {
        when(jwtDecoder.decode("responsable"))
                .thenReturn(token("responsable", "AGENT"));

        mvc.perform(post("/api/v1/transports/test")
                        .header("Authorization", "Bearer responsable"))
                .andExpect(status().isOk());
    }

    @Test
    void serviceCamionsConsulteSeulementLesTransportsActifsDuCamion()
            throws Exception {
        when(jwtDecoder.decode("camions"))
                .thenReturn(token("camions", "SERVICE_CAMIONS"));

        mvc.perform(get("/api/v1/transports")
                        .param("camionId", "camion-test")
                        .param("actifs", "true")
                        .header("Authorization", "Bearer camions"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/transports")
                        .header("Authorization", "Bearer camions"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/transports/test")
                        .header("Authorization", "Bearer camions"))
                .andExpect(status().isForbidden());
    }
}