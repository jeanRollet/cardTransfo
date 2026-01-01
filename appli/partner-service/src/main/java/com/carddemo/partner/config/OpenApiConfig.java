package com.carddemo.partner.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8085}")
    private int serverPort;

    @Bean
    public OpenAPI partnerServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CardDemo Partner API Gateway")
                        .description("""
                            ## Partner Integration API

                            This API provides secure access for external partners (fintechs, merchants, processors)
                            to CardDemo banking services.

                            ### Authentication

                            All `/partner/v1/*` endpoints require an API key in the `X-API-Key` header.

                            ```
                            X-API-Key: pk_live_your_api_key_here
                            ```

                            ### Rate Limiting

                            - **Per-minute limit**: Varies by partner tier (default: 60 requests/minute)
                            - **Daily quota**: Varies by partner tier (default: 10,000 requests/day)

                            Rate limit headers are included in responses:
                            - `X-RateLimit-Limit`: Maximum requests allowed
                            - `X-RateLimit-Remaining`: Remaining requests in current window
                            - `X-RateLimit-Reset`: Unix timestamp when the limit resets

                            ### Scopes

                            API keys can be restricted to specific scopes:
                            - `accounts:read` - Read account information
                            - `transactions:read` - Read transaction history
                            - `cards:read` - Read card information

                            ### CICS Equivalence

                            This API replaces legacy CICS Web Services and MQ Series integrations.

                            | Mainframe | Cloud Native |
                            |-----------|--------------|
                            | CICS Web Services | REST API + JSON |
                            | MQ Series | HTTP + Rate Limiting |
                            | RACF Security | API Key + Scopes |
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CardDemo API Support")
                                .email("api-support@carddemo.com"))
                        .license(new License()
                                .name("Internal Use Only")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development")))
                .components(new Components()
                        .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("Partner API Key (format: pk_live_xxxxx)")))
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"));
    }
}
