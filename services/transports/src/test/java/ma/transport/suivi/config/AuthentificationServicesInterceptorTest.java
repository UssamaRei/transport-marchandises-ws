package ma.transport.suivi.config;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthentificationServicesInterceptorTest {

    @Test
    void ajouteLeJetonTechniqueAuxRequetes() throws Exception {
        OAuth2AuthorizedClientManager manager =
                mock(OAuth2AuthorizedClientManager.class);
        OAuth2AuthorizedClient client =
                mock(OAuth2AuthorizedClient.class);

        Instant maintenant = Instant.now();
        OAuth2AccessToken jeton = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "jeton-fictif",
                maintenant,
                maintenant.plusSeconds(300)
        );

        when(manager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(client);
        when(client.getAccessToken()).thenReturn(jeton);

        HttpRequest requete = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(requete.getHeaders()).thenReturn(headers);

        ClientHttpRequestExecution execution =
                mock(ClientHttpRequestExecution.class);
        ClientHttpResponse reponse = mock(ClientHttpResponse.class);

        when(execution.execute(same(requete), any(byte[].class)))
                .thenReturn(reponse);

        AuthentificationServicesInterceptor interceptor =
                new AuthentificationServicesInterceptor(manager);

        interceptor.intercept(requete, new byte[0], execution);

        assertEquals("Bearer jeton-fictif", headers.getFirst("Authorization"));
        verify(execution).execute(same(requete), any(byte[].class));
    }

    @Test
    void sansJetonLaRequeteNestPasEnvoyee() {
        OAuth2AuthorizedClientManager manager =
                mock(OAuth2AuthorizedClientManager.class);

        when(manager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(null);

        HttpRequest requete = mock(HttpRequest.class);
        ClientHttpRequestExecution execution =
                mock(ClientHttpRequestExecution.class);

        AuthentificationServicesInterceptor interceptor =
                new AuthentificationServicesInterceptor(manager);

        assertThrows(
                IllegalStateException.class,
                () -> interceptor.intercept(requete, new byte[0], execution)
        );

        verifyNoInteractions(execution);
    }
}