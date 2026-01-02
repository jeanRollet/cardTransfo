package com.carddemo.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration for Notification Service
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CardDemo Notification Service API")
                        .description("Event-driven notification service with webhook delivery. " +
                                "Replaces MQ Series consumer patterns from the mainframe.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CardDemo Team")
                                .email("support@carddemo.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8086").description("Local Development"),
                        new Server().url("http://notification-service:8086").description("Docker")
                ));
    }
}
