package com.fulfilment.application.monolith.common;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

@OpenAPIDefinition(
    info = @Info(
        title = "Inventorix Warehouse API",
        version = "1.0.0",
        description = "Warehouse, Store and Product management — requires Bearer JWT from POST /auth/login"
    ),
    security = @SecurityRequirement(name = "bearerAuth"),
    components = @Components(
        securitySchemes = @SecurityScheme(
            securitySchemeName = "bearerAuth",
            type = SecuritySchemeType.HTTP,
            scheme = "bearer",
            bearerFormat = "JWT",
            description = "Obtain a token from POST /auth/login then paste it here"
        )
    )
)
public class OpenApiConfig {}
