package com.str.platform.application.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger documentation configuration.
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI strPlatformOpenAPI() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes("supabase-jwt",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")))
            .addSecurityItem(new SecurityRequirement().addList("supabase-jwt"))
                .info(new Info()
                        .title("STR Investment Platform API")
                        .description("REST API for short-term rental investment analysis")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Niccolo Sottile")
                                .url("https://github.com/niccolosottile")));
    }
}
