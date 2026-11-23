package ma.transport.bons.client;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

public class AuthentificationServicesInterceptor implements ClientHttpRequestInterceptor {
    private final OAuth2AuthorizedClientManager clientManager;
    public AuthentificationServicesInterceptor(OAuth2AuthorizedClientManager clientManager) { this.clientManager = clientManager; }
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("bons").principal("bons-service").build();
        OAuth2AuthorizedClient client = clientManager.authorize(authorizeRequest);
        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("Impossible d'obtenir le jeton technique Bons");
        }
        request.getHeaders().setBearerAuth(client.getAccessToken().getTokenValue());
        return execution.execute(request, body);
    }
}
