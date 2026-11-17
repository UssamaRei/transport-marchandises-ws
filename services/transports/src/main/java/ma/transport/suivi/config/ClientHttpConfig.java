package ma.transport.suivi.config;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientHttpConfig {

    @Bean
    OAuth2AuthorizedClientManager transportsAuthorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClients
    ) {
        OAuth2AuthorizedClientProvider provider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        registrations, authorizedClients
                );

        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    AuthentificationServicesInterceptor authentificationServicesInterceptor(
            OAuth2AuthorizedClientManager clientManager
    ) {
        return new AuthentificationServicesInterceptor(clientManager);
    }

    @Bean("bonsRestClient")
    public RestClient bonsRestClient(
            @Value("${services.bons.url}") String baseUrl,
            AuthentificationServicesInterceptor authentification
    ) {
        return creerClient(baseUrl, authentification);
    }

    @Bean("camionsRestClient")
    public RestClient camionsRestClient(
            @Value("${services.camions.url}") String baseUrl,
            AuthentificationServicesInterceptor authentification
    ) {
        return creerClient(baseUrl, authentification);
    }

    private RestClient creerClient(
            String baseUrl,
            AuthentificationServicesInterceptor authentification
    ) {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory() {
                    @Override
                    protected void prepareConnection(
                            HttpURLConnection connection,
                            String httpMethod
                    ) throws IOException {
                        super.prepareConnection(connection, httpMethod);

                        // Ne pas suivre une redirection avec le jeton technique.
                        connection.setInstanceFollowRedirects(false);
                    }
                };

        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor(authentification)
                .build();
    }
}