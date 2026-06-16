package com.yk.back.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Y&K Platform API",
                version = "1.0",
                description = "API REST multi-tenant — Y&K Platform",
                contact = @Contact(name = "Y&K Team", email = "support@yk-platform.com")
        ),
        servers = @Server(url = "/api", description = "Serveur local")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtenu via POST /api/auth/login"
)
public class OpenApiConfig {}
