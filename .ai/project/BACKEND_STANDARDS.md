# Padrões Backend

## Linguagem

Usar Java 21.

## Framework

Usar Spring Boot 4.x.

## Nomenclatura de pacotes

Base package:

```text
br.com.softon.portal
```

Módulos:

```text
br.com.softon.portal.gateway
br.com.softon.portal.docflow
br.com.softon.portal.releaseorchestrator
```

**Não usar `modules` no caminho do pacote.**

## Nomenclatura de classes

### Controllers

```text
ClienteController
ReleaseController
ProdutoRhController
```

### Services (classe concreta)

```text
ClienteService
ReleaseService
ProdutoRhService
```

> O projeto **não** usa o padrão interface + `*Impl`. Cada service é uma classe
> concreta anotada com `@Service`. Só introduza uma interface quando houver
> mais de uma implementação real (ex.: stubs/mocks externos, estratégias).

### Repositories

```text
ClienteRepository
ReleaseRepository
ProdutoRhRepository
```

### Entities

```text
Cliente
Release
ReleaseItem
ProdutoRh
```

### Requests

```text
ClienteRequest
ReleaseRequest
ProdutoRhRequest
AlterarStatusReleaseRequest
```

### Responses

```text
ClienteResponse
ReleaseResponse
ProdutoRhResponse
ReleaseItemResponse
```

### Exceptions

Específicas de módulo (quando necessárias):

```text
ClienteNaoEncontradoException
```

Genéricas vivem em `shared/exception/` (`BusinessException`,
`NotFoundException`). Prefira as genéricas até existir necessidade clara de
uma específica.

## Controllers

Padrão:

```java
@RestController
@RequestMapping("/api/v1/release-orchestrator/releases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
public class ReleaseController {

  private final ReleaseService releaseService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReleaseResponse criar(@Valid @RequestBody ReleaseRequest request) {
    return ReleaseResponse.from(releaseService.criar(request));
  }
}
```

Regras:

- Usar `@RestController` + `@RequestMapping`.
- Usar `@RequiredArgsConstructor` (Lombok) — nada de constructor manual.
- Prefixar a rota por módulo: `/api/v1/{modulo}/...` (ex.: `/api/v1/docflow/clientes`,
  `/api/v1/release-orchestrator/releases`). Endpoints globais (`/api/v1/auth`,
  `/api/v1/public`) ficam sem prefixo de módulo.
- Usar plural nos recursos.
- Validar com `@Valid`.
- Retornar DTOs — nunca Entity.
- Não acessar Repository diretamente.

## Services

Padrão (classe concreta):

```java
@Service
@RequiredArgsConstructor
public class ReleaseService {

  private final ReleaseRepository releaseRepository;
  private final ProdutoRhService produtoService;

  @Transactional
  public Release criar(ReleaseRequest request) {
    // regra de negócio
  }
}
```

Regras:

- Usar `@Service` + `@RequiredArgsConstructor`.
- Service concentra regras de negócio, validações de existência, transições
  de status e orquestração.
- Service chama Repository.
- Service pode chamar Services públicos de outros módulos.
- Não criar interface + `*Impl` para CRUD simples.

## Repositories

Padrão:

```java
public interface ReleaseRepository extends JpaRepository<Release, UUID> {
}
```

Regras:

- Usar `UUID` como tipo de ID.
- Sem regra de negócio no repository.
- Usar derivações (`findBy...`) para queries simples.
- Usar `@Query` (JPQL) apenas quando necessário; `nativeQuery` só para
  recursos PG específicos (ex.: full-text search).
- Specification quando houver muitos filtros opcionais (mais de 3).

## Entities

Padrão:

```java
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_release")
public class Release extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // demais campos…

  // construtor de domínio com os campos exigidos pela criação
  public Release(ProdutoRh produto, String versao, String titulo, /* ... */) {
    this.produto = produto;
    // ...
  }

  // métodos de domínio (atualizar, alterarStatus, publicar, …)
}
```

Regras:

- Usar `@Entity` + `@Table` com prefixo **`tb_`** (singular).
- Usar Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor(access = PROTECTED)`.
  `@AllArgsConstructor`/`@Builder` ficam opcionais — adicionar somente quando
  fizer sentido (records auxiliares, testes). Construtores de domínio
  customizados são bem-vindos.
- Usar `UUID` + `GenerationType.UUID`.
- Estender `AuditableEntity` (`shared/domain`) para herdar
  `createdAt/updatedAt/createdBy/updatedBy` automáticos via Spring Data JPA
  Auditing — **não** redeclarar esses campos na entidade.
- Não expor Entity na API.
- Datas: usar `OffsetDateTime` (`TIMESTAMPTZ` no banco) para timestamps com
  fuso; `LocalDate` para datas sem hora.

## DTOs

Request:

```java
public record ReleaseRequest(
    @NotNull UUID produtoId,
    @NotBlank @Size(max = 50) String versao,
    @NotBlank @Size(max = 200) String titulo,
    @NotNull TipoRelease tipo
) {}
```

Response:

```java
public record ReleaseResponse(
    UUID id,
    String versao,
    String titulo,
    ReleaseStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy
) {
  public static ReleaseResponse from(Release release) {
    return new ReleaseResponse(
        release.getId(), release.getVersao(), release.getTitulo(),
        release.getStatus(), release.getCreatedAt(), release.getUpdatedAt(),
        release.getCreatedBy(), release.getUpdatedBy());
  }
}
```

Regras:

- Preferir `record` para DTOs.
- Usar Bean Validation nos requests.
- Separar request de response.
- A conversão Entity → Response usa **método estático `from(entity)`** no
  próprio record. MapStruct **não** é usado no projeto.

## Auditoria

Campos obrigatórios em entidades de domínio:

```text
created_at
updated_at
created_by
updated_by
```

Vêm automaticamente de `AuditableEntity` (configurado via
`JpaAuditingConfig` + `AuditorAware` que lê o `SecurityContext`). Não
sobrescrever manualmente nos services.

Entities sem auditoria completa (eventos, tokens, históricos de uma ação)
podem ter apenas `created_at`/`created_by` manuais, como `AuditoriaEvento`,
`PreviewToken`, `ReleaseHistorico`.

## Paginação

Endpoints de listagem aceitam:

```text
?page=1&size=20&sort=createdAt&direction=DESC
```

Retorno usa `PageResponse` do `shared`.

## Logs

Logar em pontos importantes:

- criação / atualização / exclusão sensível
- publicação
- erro de integração
- execução de comando administrativo

Evitar logar:

- senha
- token
- segredo
- conteúdo sensível
