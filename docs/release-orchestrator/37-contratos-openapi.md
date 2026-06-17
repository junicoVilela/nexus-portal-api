# 37 — Contratos OpenAPI

Spec da geração e uso de OpenAPI 3 (Swagger) no orchestrator. Aplicação prática para garantir contrato estável entre back e front.

---

## 1. Por que OpenAPI

- **Frontend gera clients tipados** automaticamente (`orval`, `openapi-typescript-codegen`).
- **Documentação interativa** via Swagger UI.
- **Contract testing**: comparar OpenAPI atual com o que o frontend espera.
- **Versionamento**: spec é artefato versionado.

---

## 2. Stack

- `springdoc-openapi-starter-webmvc-ui` (Spring Boot 3+/4.x).
- Endpoint: `/swagger-ui.html` (UI) e `/v3/api-docs` (JSON).

### Maven
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.6.0</version>
</dependency>
```

### Configuração
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: method
  packages-to-scan: br.com.softon.portal.orchestrator
  show-actuator: false
```

---

## 3. Anotação dos endpoints

### Padrão mínimo
Cada endpoint deve ter:
- `@Operation(summary, description)`.
- `@ApiResponses` com pelo menos 200/201, 400, 401, 403, 404, 409.
- Examples nos parâmetros e body.

### Exemplo

```java
@RestController
@RequestMapping("/api/v1/orchestrator/clientes")
@Tag(name = "Clientes", description = "Gestão de clientes do orchestrator")
public class ClienteController {

    @Operation(
        summary = "Criar novo cliente",
        description = "Cria um novo cliente. CNPJ deve ser único."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cliente criado",
            content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validação falhou",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "CNPJ duplicado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse criar(
        @Valid @RequestBody
        @Parameter(description = "Dados do cliente",
                   example = "{ \"nome\": \"Cliente Teste\", ... }")
        ClienteRequest request
    ) { ... }
}
```

---

## 4. Schemas

### Documentar campos

```java
public record ClienteRequest(
    @Schema(description = "Nome ou razão social", example = "Cliente Teste LTDA")
    @NotBlank @Size(max = 200)
    String nome,

    @Schema(description = "CNPJ no formato XX.XXX.XXX/XXXX-XX", example = "12.345.678/0001-99")
    @NotBlank @Pattern(regexp = "^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$")
    String cnpj,

    @Schema(description = "Sigla curta para uso interno", example = "CLI")
    @NotBlank @Size(max = 20)
    String sigla,

    // ...
) {}
```

### Tipos polimórficos
Para `configEspecifica` de `ModuloProduto`:

```java
@Schema(
    discriminatorProperty = "tipo",
    oneOf = { ConfigWeb.class, ConfigBanco.class, ConfigKettle.class, ConfigGerado.class }
)
public interface ConfigEspecifica {}
```

---

## 5. Tags (agrupamento)

Tags principais sugeridas:
- `Clientes` — endpoints de cliente.
- `Configuração de Entrega` — config + credenciais.
- `Domínios e Funcionalidades`.
- `Produtos Contratados`.
- `Próximas Entregas` — agenda.
- `Entregas` — execução, histórico, reentrega.
- `Pacotes` — geração, download.
- `Relatórios`.
- `Auditoria`.

---

## 6. Segurança (JWT)

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Softon Portal — Release Orchestrator API")
            .version("1.0.0")
            .description("API para orquestração de entregas a clientes."))
        .components(new Components()
            .addSecuritySchemes("bearer-jwt",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
}
```

Resultado: Swagger UI mostra botão "Authorize" para colar o token.

---

## 7. Versionamento da API

### Estratégia
- URL versionada: `/api/v1/orchestrator/...`.
- Breaking changes → `/api/v2/`.
- Não-breaking: campos adicionais, valores de enum, novos endpoints — não bump version.

### Sinalizar `@Deprecated`
```java
@Deprecated
@Operation(summary = "[DEPRECATED] Listar clientes", deprecated = true)
@GetMapping("/legacy")
public ... { ... }
```

---

## 8. Geração de client TypeScript

### Stack sugerida: `orval`

`orval.config.js`:
```js
module.exports = {
  orchestrator: {
    input: 'http://localhost:8080/v3/api-docs',
    output: {
      mode: 'split',
      target: 'src/app/modules/orchestrator/api/generated.ts',
      client: 'angular',
      override: {
        mutator: {
          path: 'src/app/shared/http/api-mutator.ts',
          name: 'apiMutator'
        }
      }
    }
  }
};
```

### Gerar
```bash
npx orval
```

### Resultado
Gera services tipados, hooks, modelos. Frontend importa e usa:

```ts
import { getEntregas, createEntrega } from '@modules/orchestrator/api/generated';
```

---

## 9. Validação contínua de contrato

### Como evitar drift

#### Opção A — Pre-commit hook
- Hook `pre-commit` regenera o client TS.
- Se mudou, force commit do arquivo gerado.
- Garante que o front sempre tem o último contrato.

#### Opção B — CI check
- Job que compara `openapi.yaml` commit anterior vs HEAD.
- Falha se houver breaking change não anotada.
- Stack: `openapi-diff`.

#### Opção C — Contract test
- Backend gera spec em build time.
- Frontend tem snapshot do spec esperado.
- CI compara — falha se divergente.
- Stack: `Pact` (mais elaborado).

### Recomendação MVP
- Opção A (mais simples).
- Migrar para B/C quando o time crescer.

---

## 10. Exportação para arquivo

### Em build
```xml
<plugin>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>integration-test</id>
      <goals>
        <goal>generate</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <outputFileName>openapi.yaml</outputFileName>
    <outputDir>${project.basedir}/docs/api</outputDir>
  </configuration>
</plugin>
```

Gera `docs/api/openapi.yaml` em cada build. Versionado em git.

---

## 11. Exemplos de chamadas

### cURL gerado pelo Swagger UI
```bash
curl -X POST 'https://intranet.softon.com.br/api/v1/orchestrator/clientes' \
  -H 'Authorization: Bearer eyJhbGc...' \
  -H 'Content-Type: application/json' \
  -d '{
    "nome": "Cliente Teste LTDA",
    "cnpj": "12.345.678/0001-99",
    "sigla": "TESTE"
  }'
```

### Postman / Bruno
Importar `openapi.yaml` diretamente. Coleções geradas com todos os endpoints.

---

## 12. Documentação cruzada

### O que NÃO documentar no OpenAPI
- Regras de negócio detalhadas (vai para os specs `.md`).
- Diagramas de fluxo (vai para `.md`).
- Decisões de arquitetura (ADRs).

### O que DOCUMENTAR no OpenAPI
- Contratos (request/response).
- Exemplos.
- Códigos HTTP esperados.
- Authentication.
- Validações de campo.

### Link
Adicionar no header do Swagger UI:
```java
.description("Documentação técnica completa em: https://github.com/softon/portal-api/tree/main/docs/release-orchestrator")
```

---

## 13. Roadmap de implementação

| Fase | Item | Esforço |
|---|---|---|
| F0 | Adicionar `springdoc-openapi` | S |
| F0 | Anotar 80% dos endpoints | M |
| F1 | Anotar 100% + examples completos | M |
| F1 | Gerar client TS no frontend | M |
| F2 | CI check de breaking changes | M |
| F3 | Contract testing com Pact | L |

---

## 14. Cross-reference

- Endpoints atuais (manuais) — consolidados neste documento.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Problem Details.
- [`99-melhorias-sugeridas.md`](99-melhorias-sugeridas.md) — Seção B.8 e I.1.
- [`99-melhorias-sugeridas.md`](99-melhorias-sugeridas.md) — Backlog do backend.
