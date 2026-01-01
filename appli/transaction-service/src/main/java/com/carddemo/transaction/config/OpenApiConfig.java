package com.carddemo.transaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration
 *
 * Configures Swagger/OpenAPI documentation for the Transaction Service.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CardDemo Transaction Service API")
                        .description("Transaction history and reporting service. " +
                                "Replaces CICS transactions COTRN00C (Transaction History) and CORPT00C (Reports).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CardDemo Team")
                                .email("carddemo@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Current server")
                ));
    }
}
