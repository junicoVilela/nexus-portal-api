# Arquitetura Backend

## Estilo arquitetural

Monólito modular simples.

Cada módulo Maven representa uma área funcional e concentra suas classes de
controller, service, repository, entity, dto e exception em um único pacote
flat por camada.

## Estrutura Maven

```text
softon-portal-api/
├── shared/             ← módulo Maven compartilhado (todos os módulos dependem)
├── application/        ← módulo Spring Boot runnable (boot class + application.yml)
├── doc-flow/           ← módulo Maven do docflow (depende de shared)
├── release-orchestrator/       ← módulo Maven do release-orchestrator (depende de shared)
├── gateway/            ← módulo Maven do gateway (depende de shared)
└── pom.xml
```

`application/` contém:

- `SoftonPortalApplication` com `@SpringBootApplication(scanBasePackages = "br.com.softon.portal")`.
- `src/main/resources/application.yml` (+ `application-dev.yml`, `application-prod.yml`).

## Pacote raiz

```text
br.com.softon.portal
```

## Regra de pacote

Cada módulo de negócio usa o padrão:

```text
br.com.softon.portal.{modulo}.{camada}
```

**Não usar `modules` no caminho do pacote.**

Correto:

```text
br.com.softon.portal.docflow.controller
br.com.softon.portal.releaseorchestrator.service
br.com.softon.portal.docflow.entity
```

Errado:

```text
br.com.softon.portal.modules.docflow.controller   // ERRADO
```

## Estrutura padrão de módulo

```text
{modulo}/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
│   ├── request/
│   └── response/
└── exception/   ← opcional; criar só se houver exceção específica do módulo
```

`mapper/` não é usado — a conversão Entity → Response é feita por método
estático `from(entity)` no próprio record de Response.

## Responsabilidades

### Controller

- Recebe requisições HTTP.
- Valida entrada com `@Valid`.
- Chama Service.
- Retorna DTO de resposta.
- Não contém regra de negócio.
- Não acessa Repository diretamente.
- Não retorna Entity.

### Service

- Classe concreta anotada com `@Service` + `@RequiredArgsConstructor`.
- Contém regras de negócio, orquestração e validação de estado.
- Chama Repository.
- Pode chamar Services públicos de outros módulos quando necessário.
- Não criar interface + `*Impl` para CRUD simples — só introduza interface
  quando houver mais de uma implementação real.

### Repository

- Acesso ao banco via Spring Data JPA.
- Sem regra de negócio.
- Pode conter queries de consulta (`@Query`/Specification).
- Não deve ser usado diretamente pelo Controller.

### Entity

- Representa entidade JPA.
- Tabela com prefixo `tb_` (singular).
- Usa Lombok: `@Getter @Setter @NoArgsConstructor(access = PROTECTED)`.
- Construtor de domínio opcional para garantir invariantes na criação.
- Estende `AuditableEntity` para herdar auditoria automática.
- Não é exposta diretamente pela API.

### DTO

- Record para Request e Response.
- Request usa Bean Validation.
- Response declara um `from(entity)` estático para fazer a conversão.

### Mapper

- Não usado no projeto. A conversão fica no método `from(entity)` do record
  de Response.

### Exception

- Específicas do módulo ficam em `{modulo}/exception/` quando justificam um
  tipo dedicado.
- Genéricas (`BusinessException`, `NotFoundException`) ficam em
  `shared/exception/`.
- Tratamento centralizado em `GlobalExceptionHandler` (no `shared`).

## Comunicação entre módulos

Permitido:

- Um Service chamar outro Service público de outro módulo.
- Um módulo usar DTO público de outro módulo, se necessário.

Evitar:

- Um módulo acessar Repository de outro módulo.
- Dependências circulares entre módulos.

Exemplo ruim:

```java
@Service
@RequiredArgsConstructor
public class ClienteService {
    private final ModuloRepository moduloRepository; // ERRADO — repositório de outro módulo
}
```

Exemplo correto:

```java
@Service
@RequiredArgsConstructor
public class ClienteService {
    private final ModuloService moduloService; // CORRETO
}
```

## Shared

A pasta `shared` contém apenas itens genéricos:

```text
br.com.softon.portal.shared.config     ← SecurityConfig, JwtService, JwtAuthFilter,
                                         JpaAuditingConfig, StorageProperties…
br.com.softon.portal.shared.exception  ← GlobalExceptionHandler, BusinessException,
                                         NotFoundException
br.com.softon.portal.shared.api        ← PageResponse, PageableUtils, SortUtils
br.com.softon.portal.shared.domain     ← AuditableEntity (createdAt/updatedAt/
                                         createdBy/updatedBy + AuditingEntityListener)
br.com.softon.portal.shared.util       ← SlugUtils…
```

Não colocar regras de negócio específicas em `shared`.

## Auditoria

`AuditableEntity` (em `shared/domain`) carrega:

- `createdAt`, `updatedAt` (`OffsetDateTime`, mapeados como `TIMESTAMPTZ`)
- `createdBy`, `updatedBy` (`String`)

Preenchimento automático via Spring Data JPA Auditing:

- `JpaAuditingConfig` em `shared/config` habilita `@EnableJpaAuditing` e
  registra um `AuditorAware<String>` que lê o `SecurityContext`
  (`"system"` como fallback).
- `AuditableEntity` usa `@EntityListeners(AuditingEntityListener.class)`
  e as annotations `@CreatedDate`/`@LastModifiedDate`/
  `@CreatedBy`/`@LastModifiedBy`.

Toda entity de domínio principal deve estender `AuditableEntity`. Entities
de evento/histórico/token que só precisam de `created_at`/`created_by`
podem manter os campos manuais (`@PrePersist`).

## Quando não criar abstrações

Não criar abstrações quando:

- O CRUD é simples.
- Existe apenas uma implementação.
- A abstração não reduz acoplamento real.

Evitar excesso de:

- Interface para tudo (incluindo services CRUD).
- UseCase para tudo.
- Manager, Helper e Util genéricos.

## Decisão padrão

CRUD simples:

```text
Controller -> Service -> Repository
```

Regra mais complexa:

```text
Controller -> Service principal -> Services auxiliares -> Repository
```
