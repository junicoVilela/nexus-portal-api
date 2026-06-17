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
  - `releaseorchestrator.artefatos.dir` — raiz dos artefatos uploadados em releases (ex.: `/var/lib/softon/artefatos`).
  - `orchestrator.pacotes.dir` — raiz dos pacotes gerados pelo Orchestrator (ex.: `/var/lib/softon/pacotes`).
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

### Roles (Spring Security)
Reaproveitar as roles existentes do release-orchestrator e adicionar uma terceira:
- `ROLE_ADMIN` — tudo.
- `ROLE_EDITOR` — operação do dia a dia.
- `ROLE_VIEWER` — somente leitura. **Novo** (o backend hoje só tem ADMIN/EDITOR; será adicionado).

### Permissões internas (granulares)
Em vez de apenas `@PreAuthorize("hasRole(...)")`, adicionar permissões nomeadas para casos sensíveis. Implementação: enum `Permissao` + bean `Map<Role, Set<Permissao>>` + helper `@auth.has('...')` usado nos controllers.

| Permissão | ADMIN | EDITOR | VIEWER |
|---|---|---|---|
| `cliente.consultar` | ✓ | ✓ | ✓ |
| `cliente.criar` | ✓ | ✓ |  |
| `cliente.editar` | ✓ | ✓ |  |
| `cliente.excluir` | ✓ |  |  |
| `cliente.config-entrega` | ✓ |  |  |
| `produto.consultar` | ✓ | ✓ | ✓ |
| `produto.criar` | ✓ |  |  |
| `produto.editar` | ✓ | ✓ |  |
| `modulo.gerenciar` | ✓ | ✓ |  |
| `release.consultar` | ✓ | ✓ | ✓ |
| `release.criar` | ✓ | ✓ |  |
| `release.publicar` | ✓ |  |  |
| `release.cancelar` | ✓ |  |  |
| `artefato.upload` | ✓ | ✓ |  |
| `artefato.remover-publicada` | ✓ |  |  |
| `entrega.consultar` | ✓ | ✓ | ✓ |
| `entrega.gerar` | ✓ | ✓ |  |
| `entrega.cancelar` | ✓ |  |  |
| `entrega.reentregar` | ✓ | ✓ |  |
| `entrega.republicar` | ✓ |  |  |
| `configuracoes.editar` | ✓ |  |  |
| `auditoria.consultar` | ✓ |  | ✓ |

### Mudança necessária no release-orchestrator existente
- Adicionar `ROLE_VIEWER` aos endpoints de leitura (`GET ...`).
- Aplicar permissões granulares aos endpoints críticos (`publicar`, `cancelar`, `excluir`).

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
    dir: /var/lib/softon/artefatos
    tamanho-maximo-mb: 500
  pdf:
    snapshot-dir: /var/lib/softon/pdfs
    cache-rendered:
      enabled: true
      ttl-minutes: 60

orchestrator:
  pacotes:
    dir: /var/lib/softon/pacotes
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
- Roles e permissões base — consolidado neste módulo.
- Tratamento de erro / Problem Details — ver [`99-padroes-tela.md`](99-padroes-tela.md).
