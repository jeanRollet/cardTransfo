package com.carddemo.card.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration for Card Service
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CardDemo - Card Management API")
                        .version("1.0.0")
                        .description("""
                            Credit Card Management REST API.

                            **CICS Transaction Mapping:**
                            | CICS Transaction | REST Endpoint |
                            |-----------------|---------------|
                            | COCRDLIC (Card List) | GET /api/v1/cards/account/{accountId} |
                            | COCRDSLC (Card Select) | GET /api/v1/cards/{cardNumber} |
                            | COCRDUPC (Card Update) | PUT /api/v1/cards/{cardNumber}/status |

                            **Card Status Codes:**
                            - Y = Active
                            - N = Closed
                            - S = Blocked (Stolen/Lost)

                            **Card Types:**
                            - VC = Visa
                            - MC = Mastercard
                            - AX = American Express
                            - DC = Discover
                            """)
                        .contact(new Contact()
                                .name("CardDemo Team")
                                .email("carddemo@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Default Server")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token from auth-service")));
    }
}
