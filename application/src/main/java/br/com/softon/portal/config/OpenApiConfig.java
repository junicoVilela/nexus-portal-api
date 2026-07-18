package br.com.softon.portal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  @Bean
  OpenAPI softonPortalOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("Softon Portal API")
            .description("Contratos dos módulos DTEC Flow, Segurança e Release Orchestrator.")
            .version("1.0.0")
            .contact(new Contact().name("Softon Tecnologia")))
        .servers(List.of(new Server().url("/").description("Origem atual")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
        .components(new Components().addSecuritySchemes(BEARER_AUTH,
            new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")));
  }
}
