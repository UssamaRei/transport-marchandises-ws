package ma.transport.chargements.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "ma.transport.chargements.proxy")
public class ProjectConfig {
}
