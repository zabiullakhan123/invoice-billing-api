package com.example.billing.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui.html. Log in via POST /api/auth/login, then click
 * "Authorize" and paste the accessToken to call protected endpoints.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Invoice & Billing API", version = "1.0",
                description = "Customers, invoices with tax/discount calculation, and payment tracking"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
