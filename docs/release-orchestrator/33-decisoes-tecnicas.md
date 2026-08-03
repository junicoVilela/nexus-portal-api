# 33 — Decisões Técnicas (MVP)

Decisões fechadas para o primeiro ciclo de desenvolvimento. Itens marcados *(pós-MVP)* ficam para revisão futura.

## Stack

### PDF (Markdown/HTML → PDF)
- **Renderer**: Thymeleaf (templates HTML) + **openhtmltopdf** (HTML → PDF).
- **Conversão Markdown → HTML** (quando o conteúdo-fonte for Markdown, como em `ReleaseTemplate`): `commonmark-java`.
- **Localização dos templates**: classpath em `release-orchestrator/src/main/resources/templates/pdf/`.
- **Fonte com suporte a UTF-8** (ex.: Liberation Sans ou Noto Sans) em `resources/fonts/`, registrada no `PdfRendererBuilder`.

### Storage de artefatos uploadados e pacotes
- Filesystem local, path configurável via Spring properties.
- **Chaves**:
  - `releaseorchestrator.artefatos.dir` — raiz dos artefatos uploadados em releases (ex.: `/var/lib/nexus/artefatos`).
  - `orchestrator.pacotes.dir` — raiz dos pacotes gerados pelo Orchestrator (ex.: `/var/lib/nexus/pacotes`).
- **Convenção de path**:
  - Artefatos: `{releaseorchestrator.artefatos.dir}/{releaseId}/{moduloId}/{sha256}-{nomeOriginal}`.
  - Pacotes: `{orchestrator.pacotes.dir}/{entregaId}/...`.
- Processo Spring Boot precisa ter permissão de escrita nas raízes.

### Geração assíncrona
- `@EnableAsync` + `@Async` do Spring.
- `ThreadPoolTaskExecutor` em `OrchestratorAsyncConfig`:
  - `corePoolSize`: 2
  - `maxPoolSize`: 4
  - `queueCapacity`: 25
  - `threadNamePrefix`: `orch-gen-`
- Cada geração roda em uma thread. Cancelamento via flag de interrupção checada entre etapas.

### Status de geração (execução da entrega)
```java
public enum GeracaoStatus {
    PENDENTE,
    PROCESSANDO,
    CONCLUIDO,
    ERRO,
    CANCELADO
}
```
- Substitui o `EntregaStatus` proposto anteriormente em `32`.
- **Reentrega não é status** — é uma nova `Entrega` com FK `entregaOriginalId` apontando para a anterior. Reentrega pode reutilizar o pacote já gerado (sem refazer a geração) ou disparar nova geração.

---

## Autorização

O backend usa **dois mecanismos coexistentes**, já implementados em `doc-flow`:

1. **Roles legados** (gate efetivo nos controllers) — string CSV em `Usuario.roles`, carregada no JWT, consumida por `@PreAuthorize("hasAnyRole(...)")`.
2. **RBAC tabular** (catálogo fino + UI) — entidades persistidas em `doc-flow` (módulo lógico `rbac`), retornadas em `GET /api/v1/auth/me` para a UI decidir o que mostrar.

Os dois andam juntos. A migração completa para RBAC table-driven nos `@PreAuthorize` é evolução pós-MVP.

### Roles legados (Spring Security — gate atual)

- `ROLE_ADMIN` — tudo.
- `ROLE_EDITOR` — operação do dia a dia.
- `ROLE_LEITOR` — somente leitura. **Novo** (o backend hoje só tem `ADMIN`/`EDITOR` ativos via `@PreAuthorize`; precisa ser adicionado nos endpoints `GET ...` do release-orchestrator).

Implementação atual:
```java
// Usuario.roles é String CSV ("ADMIN,EDITOR")
@Column(nullable = false, length = 60) private String roles;

// JWT carrega a lista
public List<String> roleList() { return Arrays.stream(roles.split(",")).map(String::trim).filter(r -> !r.isBlank()).toList(); }

// Controller efetivo
@PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
```

`JwtAuthFilter` / `GatewayAuthFilter` prefixam `ROLE_` automaticamente.

### RBAC tabular (catálogo + UI)

Já existe em `doc-flow` (migrations `V7–V13` — ver [`../MIGRATIONS.md`](../MIGRATIONS.md)):

| Tabela | Conteúdo |
|---|---|
| `tb_dominio` | Domínio de permissão (`SEGURANCA`, `SISTEMA`, `DOC_FLOW`, `RELEASE_ORCHESTRATOR`) |
| `tb_funcionalidade` | Funcionalidade dentro de um domínio (`USUARIO`, `CLIENTE`, `RELEASE`, `PRODUTO`, `TEMPLATE`, …) |
| `tb_permissao` | Permissão atômica com código `FUNCIONALIDADE:ACAO` (`RELEASE:CRIAR`, `CLIENTE:LER`) |
| `tb_grupo` | Grupo (seed: `ADMIN`, `EDITOR`, `LEITOR`) |
| `tb_grupo_permissao` | N:N grupo↔permissão |
| `tb_grupo_usuario` | N:N grupo↔usuário |

> ⚠️ Colisão de nome: `tb_dominio` / `tb_funcionalidade` já estão ocupadas pelo RBAC. O catálogo funcional **do produto** (`05`/`11`) usa entidades distintas no orchestrator — `DominioProduto` / `FuncionalidadeProduto` (`tb_dominio_produto` / `tb_funcionalidade_produto`). Ver [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md).

#### Convenção de ação

| Ação CRUD | Significado |
|---|---|
| `LER` | Consultar |
| `CRIAR` | Criar novo |
| `EDITAR` | Atualizar existente |
| `EXCLUIR` | Remover/inativar |

**Especiais** (fora do CRUD): `VINCULAR_PERMISSAO`, `BLOQUEAR`, `RESETAR_SENHA`, `REVOGAR`, `VISUALIZAR` (auditoria/histórico-login).

#### Permissões do orchestrator (catálogo a semear)

Códigos no formato `FUNC:ACAO`. As permissões CRUD são geradas via `CROSS JOIN` no seed (V10) para todas as funcionalidades ativas; aqui ficam as **especiais** + as que precisam aparecer no plano:

| Permissão | ADMIN | EDITOR | LEITOR | Observações |
|---|---|---|---|---|
| `CLIENTE:LER` | ✓ | ✓ | ✓ | CRUD padrão |
| `CLIENTE:CRIAR` | ✓ | ✓ |  | |
| `CLIENTE:EDITAR` | ✓ | ✓ |  | |
| `CLIENTE:EXCLUIR` | ✓ |  |  | |
| `CLIENTE:CONFIG_ENTREGA` | ✓ |  |  | Especial (a semear) |
| `PRODUTO:LER` | ✓ | ✓ | ✓ | |
| `PRODUTO:CRIAR` | ✓ |  |  | |
| `PRODUTO:EDITAR` | ✓ | ✓ |  | |
| `MODULO:GERENCIAR` | ✓ | ✓ |  | Cobre catálogo §10 |
| `RELEASE:LER` | ✓ | ✓ | ✓ | |
| `RELEASE:CRIAR` | ✓ | ✓ |  | |
| `RELEASE:PUBLICAR` | ✓ |  |  | Especial |
| `RELEASE:CANCELAR` | ✓ |  |  | Especial |
| `ARTEFATO:UPLOAD` | ✓ | ✓ |  | Especial |
| `ARTEFATO:REMOVER_PUBLICADA` | ✓ |  |  | Especial |
| `ENTREGA:LER` | ✓ | ✓ | ✓ | |
| `ENTREGA:GERAR` | ✓ | ✓ |  | Especial |
| `ENTREGA:CANCELAR` | ✓ |  |  | Especial |
| `ENTREGA:REENTREGAR` | ✓ | ✓ |  | Especial |
| `ENTREGA:REPUBLICAR` | ✓ |  |  | Especial |
| `CONFIGURACAO:EDITAR` | ✓ |  |  | |
| `AUDITORIA:VISUALIZAR` | ✓ |  | ✓ | Já no catálogo |

### Como o RBAC entra na requisição

```text
Login → JWT (roles CSV) → JwtAuthFilter → SecurityContext(roles)
                                              ↓
                                       @PreAuthorize hasAnyRole(...)
                                              ↓
                                            Controller

GET /auth/me → MeResponse { roles, grupos[], permissoes[] }
                ↑                       ↑           ↑
              legacy                RbacService.gruposDoUsuario()
                                   RbacService.permissoesDoUsuario()
```

O frontend usa `permissoes[]` para esconder ações; o gate efetivo continua nos `@PreAuthorize`. Evolução: substituir `hasAnyRole` por `@auth.has('CLIENTE:CRIAR')` via `PermissionEvaluator` que consulta `RbacService`.

### Mudanças necessárias no release-orchestrator existente

- Endpoints `GET ...`: adicionar `LEITOR` em `hasAnyRole('ADMIN','EDITOR','LEITOR')`.
- Endpoints críticos (`publicar`, `cancelar`, `excluir`): manter `hasRole('ADMIN')`.
- Seed `V10__rbac__04_seed_catalog.sql`: adicionar permissões especiais do orchestrator (lista acima — apenas `CLIENTE`, `PRODUTO`, `RELEASE`, `TEMPLATE` estão no catálogo; faltam `ENTREGA`, `MODULO`, `ARTEFATO`, `AUDITORIA` no domínio `RELEASE_ORCHESTRATOR`).

---

## Dependências Maven (a adicionar)

No `pom.xml` parent (em `<dependencyManagement>`) ou direto no módulo onde for usado:
```xml
<dependency>
  <groupId>com.openhtmltopdf</groupId>
  <artifactId>openhtmltopdf-pdfbox</artifactId>
  <version>1.0.10</version>
</dependency>
<dependency>
  <groupId>com.openhtmltopdf</groupId>
  <artifactId>openhtmltopdf-slf4j</artifactId>
  <version>1.0.10</version>
</dependency>
<dependency>
  <groupId>org.commonmark</groupId>
  <artifactId>commonmark</artifactId>
  <version>0.22.0</version>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

> Thymeleaf é usado **apenas para gerar HTML dos templates de PDF**, não como camada de view do Spring MVC (a UI é Angular).

---

## `application.yml` exemplo
```yaml
releaseorchestrator:
  artefatos:
    dir: /var/lib/nexus/artefatos
    tamanho-maximo-mb: 500
  pdf:
    snapshot-dir: /var/lib/nexus/pdfs
    cache-rendered:
      enabled: true
      ttl-minutes: 60

orchestrator:
  pacotes:
    dir: /var/lib/nexus/pacotes
    retencao-dias: 365
  async:
    core-pool-size: 2
    max-pool-size: 4
    queue-capacity: 25
  geracao:
    timeout-minutos: 30
    retry-tentativas: 3
  publicacao:
    timeout-segundos: 600

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
```

---

## Stack adicional (descrita nos novos specs)

| Tópico | Stack | Spec |
|---|---|---|
| Observabilidade — logs | Logback + Logstash encoder | [`34`](34-observabilidade.md) |
| Observabilidade — métricas | Micrometer + Prometheus | [`34`](34-observabilidade.md) |
| Observabilidade — tracing | Micrometer Tracing + OTLP | [`34`](34-observabilidade.md) |
| Testes — unit | JUnit 5 + Mockito + AssertJ | [`35`](35-testes-qa.md) |
| Testes — integração | Spring Boot Test + Testcontainers | [`35`](35-testes-qa.md) |
| Testes — E2E | Playwright | [`35`](35-testes-qa.md) |
| Testes — carga | k6 / Gatling | [`35`](35-testes-qa.md) |
| Deploy | Docker + Kubernetes | [`36`](36-deploy-operacao.md) |
| CI/CD | GitHub Actions ou GitLab CI | [`36`](36-deploy-operacao.md) |
| OpenAPI | springdoc-openapi | [`37`](37-contratos-openapi.md) |
| Frontend client gen | orval ou openapi-typescript-codegen | [`37`](37-contratos-openapi.md) |

---

## Cross-reference

- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Entidades.
- [`34-observabilidade.md`](34-observabilidade.md) — Detalhes operacionais.
- [`35-testes-qa.md`](35-testes-qa.md) — Validação contínua.
- [`36-deploy-operacao.md`](36-deploy-operacao.md) — Operação em prod.
- [`37-contratos-openapi.md`](37-contratos-openapi.md) — Documentação de API.
- [`99-melhorias-sugeridas.md`](99-melhorias-sugeridas.md) — Backlog.
- [`../MIGRATIONS.md`](../MIGRATIONS.md) — Baseline Flyway (split por módulo) + ordem `docflow → seguranca → rbac → release_orchestrator`.
- Roles e permissões base — consolidado neste módulo.
- Tratamento de erro / Problem Details — ver [`99-padroes-tela.md`](99-padroes-tela.md).
