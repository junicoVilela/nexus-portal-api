# Gateway Spring - Padrão de Mercado

## Objetivo

Este arquivo define um padrão recomendado para um módulo/projeto de **Gateway Spring** usando **Spring Cloud Gateway**, com foco em boas práticas de mercado, simplicidade, segurança, observabilidade e manutenção.

O Gateway deve atuar como ponto de entrada das APIs, centralizando responsabilidades transversais como:

- Roteamento de requisições
- Segurança
- Autenticação/autorização
- CORS
- Logs
- Resiliência
- Reescrita de paths
- Padronização de headers
- Integração com serviços internos

---

## Quando usar Spring Cloud Gateway

Use Spring Cloud Gateway quando o projeto precisar de:

- Um ponto único de entrada para APIs.
- Roteamento para múltiplos serviços internos.
- Controle centralizado de autenticação.
- Filtros globais de request/response.
- Reescrita de URL.
- Integração com service discovery.

Evite usar Gateway complexo quando:

- Existe apenas uma API simples.
- Não há necessidade de roteamento.
- O sistema ainda é pequeno e não possui integrações externas.
- Um reverse proxy simples, como Nginx, já resolve o problema.

---

## Stack recomendada

```text
Java 21
Spring Boot 4.x
Spring Cloud Gateway
Spring Security
OAuth2 Resource Server / JWT
Spring Actuator
Docker
```

---

> Ajustar versões usando o BOM do Spring Cloud compatível com a versão do Spring Boot usada no projeto.

---

## Estrutura recomendada para projeto de Gateway

```text
nexus-gateway-api/
├── src/
│   └── main/
│       ├── java/
│       │   └── br/com/nexus/gateway/
│       │       ├── GatewayApplication.java
│       │       │
│       │       ├── config/
│       │       │   ├── GatewayRoutesConfig.java
│       │       │   ├── SecurityConfig.java
│       │       │   ├── CorsConfig.java
│       │       │   ├── RateLimitConfig.java
│       │       │   ├── ResilienceConfig.java
│       │       │   └── ObservabilityConfig.java
│       │       │
│       │       ├── filter/
│       │       │   ├── RequestCorrelationFilter.java
│       │       │   ├── RequestLoggingFilter.java
│       │       │   ├── ResponseHeaderFilter.java
│       │       │   └── AuthenticationContextFilter.java
│       │       │
│       │       ├── route/
│       │       │   ├── RouteProperties.java
│       │       │   ├── RouteDefinitionConfig.java
│       │       │   └── RouteValidator.java
│       │       │
│       │       ├── security/
│       │       │   ├── JwtAuthenticationConverterConfig.java
│       │       │   ├── PermissionChecker.java
│       │       │   └── PublicEndpointProperties.java
│       │       │
│       │       ├── exception/
│       │       │   ├── GatewayErrorHandler.java
│       │       │   └── GatewayException.java
│       │       │
│       │       ├── health/
│       │       │   └── DownstreamHealthIndicator.java
│       │       │
│       │       └── util/
│       │           ├── HeaderNames.java
│       │           └── RouteUtils.java
│       │
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── logback-spring.xml
│
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## Conceitos principais

### Route

Uma rota define para onde uma requisição será encaminhada.

Exemplo:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: docflow-api
          uri: http://localhost:8081
          predicates:
            - Path=/api/docflow/**
          filters:
            - StripPrefix=1
```

### Predicate

Predicate define a condição para uma rota ser acionada.

Exemplos comuns:

```yaml
predicates:
  - Path=/api/docflow/**
  - Method=GET,POST
  - Header=Authorization
  - Host=api.nexus.com.br
```

### Filter

Filter modifica request ou response antes/depois do encaminhamento.

Exemplos:

```yaml
filters:
  - StripPrefix=1
  - AddRequestHeader=X-Gateway, nexus-gateway
  - RemoveResponseHeader=X-Powered-By
```

---

## Configuração base recomendada

```yaml
server:
  port: 8080

spring:
  application:
    name: nexus-gateway-api

  cloud:
    gateway:
      default-filters:
        - RemoveResponseHeader=X-Powered-By
        - AddResponseHeader=X-Gateway, nexus-gateway

      routes:
        - id: docflow-api
          uri: http://localhost:8081
          predicates:
            - Path=/api/docflow/**
          filters:
            - StripPrefix=1

        - id: portal-api
          uri: http://localhost:8082
          predicates:
            - Path=/api/portal/**
          filters:
            - StripPrefix=1

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,gateway

  endpoint:
    health:
      show-details: when_authorized
```

---

## Configuração por ambiente

### application-dev.yml

```yaml
gateway:
  cors:
    allowed-origins:
      - http://localhost:4200
      - http://localhost:5173

services:
  docflow:
    url: http://localhost:8081
  portal:
    url: http://localhost:8082
```

### application-prod.yml

```yaml
gateway:
  cors:
    allowed-origins:
      - https://portal.nexus.local

services:
  docflow:
    url: http://docflow-api:8080
  portal:
    url: http://portal-api:8080
```

---

## Exemplo com variáveis de ambiente

```yaml
server:
  port: ${SERVER_PORT:8080}

services:
  docflow:
    url: ${DOCFLOW_API_URL:http://localhost:8081}
  portal:
    url: ${PORTAL_API_URL:http://localhost:8082}

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:http://localhost:8089/realms/nexus}
```

---

## Segurança

### Princípios

O Gateway deve:

- Validar autenticação.
- Repassar contexto do usuário para serviços internos.
- Bloquear endpoints protegidos.
- Permitir endpoints públicos explicitamente.
- Não expor detalhes internos.
- Não logar tokens ou dados sensíveis.
- Remover headers desnecessários.
- Aplicar CORS restrito por ambiente.

---

## Endpoints públicos sugeridos

```text
/actuator/health
/actuator/info
/api/auth/login
/api/auth/refresh
/swagger-ui/**
/v3/api-docs/**
```

> Em produção, cuidado ao expor Swagger e Actuator. Preferir restringir por rede, autenticação ou perfil.

---

## Exemplo de SecurityConfig

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(Customizer.withDefaults())
            .authorizeExchange(exchange -> exchange
                .pathMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

---

## CORS

Configuração recomendada:

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "https://*.nexus.com.br"
        ));

        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Request-Id",
            "X-Correlation-Id"
        ));

        config.setExposedHeaders(List.of(
            "X-Request-Id",
            "X-Correlation-Id"
        ));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
```

> Em produção, evite liberar `*`. Configure domínios explicitamente.

---

## Headers recomendados

### Headers recebidos

```text
Authorization
Content-Type
X-Request-Id
X-Correlation-Id
X-Tenant-Id
```

### Headers propagados

```text
X-Request-Id
X-Correlation-Id
X-User-Id
X-User-Email
X-User-Roles
```

### Headers removidos

```text
X-Powered-By
Server
X-Internal-Token
```

---

## Correlation ID

Toda requisição deve possuir um identificador único para rastreabilidade.

### Filtro sugerido

```java
@Component
public class RequestCorrelationFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest()
            .getHeaders()
            .getFirst(CORRELATION_ID);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest request = exchange.getRequest()
            .mutate()
            .header(CORRELATION_ID, correlationId)
            .build();

        exchange.getResponse()
            .getHeaders()
            .add(CORRELATION_ID, correlationId);

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

---

## Logging

### O que logar

- Método HTTP
- Path
- Status HTTP
- Tempo de resposta
- Correlation ID
- IP de origem, se necessário
- Route ID

### O que não logar

- Authorization header
- Senhas
- Tokens
- Cookies sensíveis
- Payload com dados pessoais ou sensíveis

### Exemplo de log

```text
correlationId=abc-123 method=GET path=/api/docflow/manuais status=200 durationMs=45 routeId=docflow-api
```

---

## Rate Limiting

Rate limiting deve ser aplicado principalmente em:

- Login
- APIs públicas
- APIs sensíveis
- APIs com alto custo computacional
- Endpoints sujeitos a abuso

Exemplo com Redis:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: docflow-api
          uri: http://localhost:8081
          predicates:
            - Path=/api/docflow/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                redis-rate-limiter.requestedTokens: 1
```

Controller de fallback:

```java
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/docflow")
    public Mono<ResponseEntity<Map<String, Object>>> docflowFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            Map.of(
                "success", false,
                "message", "Serviço de manuais temporariamente indisponível"
            )
        ));
    }
}
```

---

## Timeout

Configurar timeout evita conexões presas.

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 3000
        response-timeout: 10s
```

### Logs

Usar logs estruturados sempre que possível.

Campos recomendados:

```text
timestamp
level
service
correlationId
routeId
method
path
status
durationMs
userId
```

---

## Health Check

Endpoint recomendado:

```text
GET /actuator/health
```

Em ambiente interno, pode adicionar health de serviços downstream:

```text
docflow-api
portal-api
auth-server
redis
```

---

## Roteamento por configuração

Para poucos serviços, `application.yml` é suficiente.

Exemplo:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: docflow-api
          uri: ${DOCFLOW_API_URL:http://localhost:8081}
          predicates:
            - Path=/api/docflow/**
          filters:
            - StripPrefix=1
```

---

## Roteamento por Java Config

Use Java Config quando precisar de lógica mais controlada.

```java
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("docflow-api", route -> route
                .path("/api/docflow/**")
                .filters(filter -> filter
                    .stripPrefix(1)
                    .addRequestHeader("X-Gateway", "nexus-gateway")
                )
                .uri("http://localhost:8081")
            )
            .build();
    }
}
```

---

## Roteamento dinâmico

Só implemente roteamento dinâmico se realmente necessário.

Use quando:

- As rotas precisam ser alteradas sem redeploy.
- Administradores gerenciam rotas em tela.
- Existe tabela de rotas no banco.
- O Gateway precisa carregar rotas de um repositório externo.

Evite no início se:

- As rotas são poucas.
- As rotas mudam raramente.
- O projeto ainda está simples.

---

## Modelo de dados para rotas dinâmicas

Tabela sugerida:

```text
tb_gateway_route
```

Campos:

```text
id
route_id
nome
descricao
path
target_url
metodo_http
ativo
publico
ordem
created_at
updated_at
```

Tabela de logs:

```text
tb_gateway_route_log
```

Campos:

```text
id
gateway_route_id
status_http
sucesso
mensagem
tempo_resposta_ms
data_execucao
```

---

## Boas práticas de segurança

- Não expor serviços internos diretamente para a internet.
- Usar HTTPS em produção.
- Validar JWT no Gateway.
- Propagar apenas headers necessários.
- Remover headers sensíveis.
- Aplicar rate limit em endpoints críticos.
- Proteger Actuator.
- Restringir CORS.
- Não logar tokens.
- Não logar payload sensível.
- Usar timeout.
- Usar circuit breaker em serviços instáveis.
- Separar configurações por ambiente.
- Externalizar secrets.

---

## Checklist de produção

Antes de publicar:

```text
[ ] HTTPS configurado
[ ] CORS restrito
[ ] JWT validado
[ ] Endpoints públicos revisados
[ ] Actuator protegido
[ ] Swagger protegido ou desabilitado
[ ] Rate limit aplicado onde necessário
[ ] Timeout configurado
[ ] Logs com correlation ID
[ ] Tokens não aparecem nos logs
[ ] Métricas expostas para Prometheus
[ ] Health check funcionando
[ ] Rotas testadas
[ ] Headers sensíveis removidos
[ ] Configurações via variável de ambiente
[ ] Dockerfile revisado
```

---

## Dockerfile sugerido

```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## docker-compose.yml de desenvolvimento

```yaml
services:
  nexus-gateway-api:
    build: .
    container_name: nexus-gateway-api
    ports:
      - "8080:8080"
    environment:
      SERVER_PORT: 8080
      DOCFLOW_API_URL: http://docflow-api:8081
      PORTAL_API_URL: http://portal-api:8082
      JWT_ISSUER_URI: http://keycloak:8080/realms/nexus
    networks:
      - nexus-network

networks:
  nexus-network:
    driver: bridge
```

---

## Nomenclatura recomendada

### Rotas

```text
docflow-api
portal-api
auth-api
release-api
monitoramento-api
```

### Headers

```text
X-Request-Id
X-Correlation-Id
X-Gateway
X-User-Id
X-User-Email
X-User-Roles
```

### Variáveis de ambiente

```text
SERVER_PORT
DOCFLOW_API_URL
PORTAL_API_URL
JWT_ISSUER_URI
REDIS_HOST
REDIS_PORT
```

---

## O que evitar

Evite:

- Colocar regra de negócio no Gateway.
- Transformar Gateway em backend principal.
- Fazer chamadas bloqueantes sem necessidade.
- Logar payload completo.
- Liberar CORS com `*` em produção.
- Expor Actuator sem proteção.
- Criar retry em POST sem idempotência.
- Criar roteamento dinâmico cedo demais.
- Misturar responsabilidades de autenticação, cadastro e negócio no Gateway.
- Usar Gateway como substituto de segurança dos serviços internos.

---

## Diferença entre Gateway e BFF

### Gateway

Responsável por:

- Roteamento
- Segurança
- Rate limiting
- Observabilidade
- Headers
- Resiliência
- Entrada única para APIs

### BFF

Responsável por:

- Adaptar dados para o frontend
- Agregar respostas
- Reduzir chamadas do frontend
- Preparar payload específico para tela

Evite colocar lógica de tela no Gateway. Se precisar adaptar muito dado para o frontend, considere um BFF separado ou módulo específico.

---

## Prompt recomendado para IA

Use este prompt ao pedir código:

```text
Crie/ajuste o Gateway Spring seguindo o arquivo GATEWAY_SPRING_STANDARDS.md.

Regras:
- Usar Spring Cloud Gateway.
- Usar Spring Boot 4.x e Java 21.
- Aplicar segurança com JWT/OAuth2 Resource Server.
- Usar rotas em application.yml quando for simples.
- Usar filters para correlation ID, logs e headers.
- Não colocar regra de negócio no Gateway.
- Não criar complexidade desnecessária.
- Não usar arquitetura hexagonal.
- Separar config, filter, security, route, exception e health.
```

---

## Resumo da recomendação

Para um Gateway Spring padrão de mercado:

```text
Spring Cloud Gateway
+ Spring Security JWT
+ CORS restrito
+ Timeout
+ Correlation ID
+ Logs estruturados
+ Actuator
+ Configuração por ambiente
```

Comece simples com rotas em `application.yml`.

Evolua para rotas dinâmicas somente quando houver necessidade real.
