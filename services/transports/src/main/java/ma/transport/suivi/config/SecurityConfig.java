package ma.transport.suivi.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtConverter
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Liste : utilisateurs ou Camions pour un camion actif.
                        .requestMatchers(HttpMethod.GET, "/api/v1/transports")
                        .access((authentication, context) -> {
                            Authentication utilisateur = authentication.get();

                            boolean humain = aUnRole(
                                    utilisateur, "ROLE_ADMIN", "ROLE_AGENT",
                                    "ROLE_LECTEUR"
                            );

                            String[] actifs = context.getRequest()
                                    .getParameterValues("actifs");
                            String[] camions = context.getRequest()
                                    .getParameterValues("camionId");

                            boolean requeteCamions = aUnRole(
                                    utilisateur, "ROLE_SERVICE_CAMIONS"
                            )
                                    && actifs != null
                                    && actifs.length == 1
                                    && "true".equalsIgnoreCase(actifs[0])
                                    && camions != null
                                    && camions.length == 1
                                    && !camions[0].isBlank();

                            return new AuthorizationDecision(
                                    humain || requeteCamions
                            );
                        })
                        // Détails et autres lectures : utilisateurs uniquement.
                        .requestMatchers(
                                HttpMethod.GET, "/api/v1/transports/**"
                        ).hasAnyRole("ADMIN", "AGENT", "LECTEUR")
                        .requestMatchers(
                                HttpMethod.HEAD, "/api/v1/transports/**"
                        ).hasAnyRole("ADMIN", "AGENT", "LECTEUR")
                        // Modifications : ADMIN ou AGENT.
                        .requestMatchers("/api/v1/transports/**")
                        .hasAnyRole("ADMIN", "AGENT")
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                )
                .build();
    }

    private static boolean aUnRole(
            Authentication authentication,
            String... roles
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(autorite ->
                        List.of(roles).contains(autorite.getAuthority()));
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles =
                new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
            String issuer,

            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
            String jwkSetUri,

            @Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
            String audience
    ) {
        if (issuer.isBlank() || jwkSetUri.isBlank() || audience.isBlank()) {
            throw new IllegalArgumentException(
                    "La configuration JWT ne doit pas être vide"
            );
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .build();

        OAuth2TokenValidator<Jwt> issuerEtDates =
                JwtValidators.createDefaultWithIssuer(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            if (token.getAudience() != null
                    && token.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Audience invalide", null)
            );
        };

        OAuth2TokenValidator<Jwt> expirationObligatoire = token -> {
            if (token.getExpiresAt() != null) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Expiration manquante", null)
            );
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerEtDates, audienceValidator, expirationObligatoire
        ));

        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(
                List.of("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
        );
        config.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept")
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}