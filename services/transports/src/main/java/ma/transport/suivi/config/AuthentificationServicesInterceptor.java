package ma.transport.suivi.config;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

public class AuthentificationServicesInterceptor
        implements ClientHttpRequestInterceptor {

    private final OAuth2AuthorizedClientManager clientManager;

    public AuthentificationServicesInterceptor(
            OAuth2AuthorizedClientManager clientManager
    ) {
        this.clientManager = clientManager;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        OAuth2AuthorizeRequest demande =
                OAuth2AuthorizeRequest
                        .withClientRegistrationId("transports")
                        .principal("transports-service")
                        .build();

        OAuth2AuthorizedClient client = clientManager.authorize(demande);

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException(
                    "Impossible d'obtenir le jeton technique Transports"
            );
        }

        request.getHeaders().setBearerAuth(
                client.getAccessToken().getTokenValue()
        );

        return execution.execute(request, body);
    }
}