package ma.transport.bons.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import ma.transport.bons.client.AuthentificationServicesInterceptor;
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
    OAuth2AuthorizedClientManager bonsAuthorizedClientManager(ClientRegistrationRepository registrations,
                                                                 OAuth2AuthorizedClientService authorizedClients) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClients);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    AuthentificationServicesInterceptor authentificationServicesInterceptor(OAuth2AuthorizedClientManager manager) {
        return new AuthentificationServicesInterceptor(manager);
    }

    @Bean("chargementsRestClient")
    RestClient chargementsRestClient(@Value("${services.chargements.url}") String baseUrl,
                                     AuthentificationServicesInterceptor auth,
                                     @Value("${services.chargements.connect-timeout}") int connectTimeout,
                                     @Value("${services.chargements.read-timeout}") int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).requestInterceptor(auth).build();
    }
}
