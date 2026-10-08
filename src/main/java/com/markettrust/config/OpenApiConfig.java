package com.markettrust.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI marketTrustOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, jwtSecurityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("MarketTrust API")
                .description("""
                        RESTful backend API for the MarketTrust peer-to-peer marketplace platform.
                        
                        Provides endpoints for product listings, seller management, real-time messaging,
                        trust & safety reviews, credit transactions, and AI-powered search.
                        
                        **Authentication:** All protected endpoints require a JWT Bearer token obtained
                        via `POST /api/auth/login`. Tokens expire after 15 minutes; use the refresh
                        endpoint to obtain a new access token.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("MarketTrust Team")
                        .email("api@markettrust.com")
                        .url("https://markettrust.com")
                )
                .license(new License()
                        .name("Proprietary")
                        .url("https://markettrust.com/terms")
                );
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(BEARER_AUTH_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Provide the JWT access token received from the /api/auth/login endpoint.");
    }
}
