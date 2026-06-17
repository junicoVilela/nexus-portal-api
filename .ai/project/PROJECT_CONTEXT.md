# Contexto Geral do Backend

## Nome do projeto

Softon Portal API

## Objetivo

Backend do portal interno corporativo da Softon. Centraliza aplicações
internas, módulos administrativos e ferramentas de apoio ao
desenvolvimento, operação e documentação.

Módulos atuais:

- Gateway (`gateway/`)
- DocFlow (`doc-flow/`) — Clientes, Projetos, Módulos, Páginas,
  Publicações, Preview, Auditoria, Usuários, Empresa, Grupos
- Release Orchestrator (`release-orchestrator/`) — Produtos RH, Releases, Itens, Histórico,
  Templates. Spec em `docs/release-orchestrator/`.

Módulos previstos:

- Release Orchestrator (Clientes, Entregas, Pacotes) — spec em
  `docs/release-orchestrator/`
- Sistemas
- Controle de Acesso
- Monitoramento
- Notificações
- Configurações

## Tipo de arquitetura

Monólito modular simples com múltiplos módulos Maven.

Não usar:

- Arquitetura Hexagonal
- Ports and Adapters
- DDD pesado
- Use cases para CRUD simples
- Microsserviços prematuros

Usar:

- Separação por módulos de negócio (cada módulo = um módulo Maven).
- Camadas internas simples (controller, service, repository, entity, dto).
- Fluxo: Controller → Service → Repository → Database.
- Lombok (`@Getter @Setter @NoArgsConstructor @RequiredArgsConstructor`)
  para reduzir boilerplate.
- Services como classes concretas (sem interface + `*Impl`).
- DTO ↔ Entity convertidos via método estático `from(entity)` no record
  de Response (MapStruct não é usado).
- Auditoria automática via Spring Data JPA Auditing
  (`AuditableEntity` no `shared/domain`).

## Stack principal

- Java 21
- Spring Boot 4.x
- Spring Web
- Spring Data JPA (com `@EnableJpaAuditing`)
- Spring Security
- PostgreSQL
- Flyway
- Bean Validation
- OpenAPI / Swagger
- Lombok
- Docker / Docker Compose
- Thymeleaf + OpenHTMLtoPDF para templates/relatórios

## Estrutura Maven

```text
softon-portal-api/
├── shared/             ← módulo Maven compartilhado (todos os módulos dependem)
├── application/        ← módulo Spring Boot runnable (SoftonPortalApplication + application.yml)
├── doc-flow/           ← módulo Maven do docflow (depende de shared)
├── release-orchestrator/       ← módulo Maven do release-orchestrator (depende de shared)
├── gateway/            ← módulo Maven do gateway (depende de shared)
└── pom.xml             ← parent pom
```

`application.yml` (+ profiles `dev`/`prod`) ficam em
`application/src/main/resources/`.

## Pacote raiz e convenção

```text
br.com.softon.portal
```

Padrão de pacote por módulo:

```text
br.com.softon.portal.{modulo}.{camada}
```

Exemplos:

```text
br.com.softon.portal.docflow.controller
br.com.softon.portal.docflow.service
br.com.softon.portal.docflow.repository
br.com.softon.portal.docflow.entity
br.com.softon.portal.docflow.dto.request
br.com.softon.portal.docflow.dto.response
br.com.softon.portal.shared.config
br.com.softon.portal.shared.exception
br.com.softon.portal.shared.domain
```

**Não usar `modules` no caminho do pacote.**

## Convenção de URL

```text
/api/v1/{modulo}/recursos    ← endpoints privados (docflow, release-orchestrator, gateway)
/api/v1/auth/...             ← global
/api/v1/public/...           ← global público
```

## Convenção de tabelas

Tabelas com prefixo `tb_` (singular):

```text
tb_cliente, tb_projeto, tb_pagina, tb_publicacao, tb_release, tb_release_item…
```

## Tom da resposta esperada da IA

Ao ajudar neste projeto, a IA deve:

- Ser objetiva.
- Priorizar simplicidade.
- Evitar complexidade desnecessária.
- Gerar código compatível com monólito modular simples.
- Explicar decisões técnicas quando necessário.
- Usar nomes em português quando fizer sentido.
- Manter a estrutura do projeto consistente.
- Nunca adicionar `modules` no caminho do pacote.
- Nunca introduzir MapStruct, interface+Impl em services CRUD, ou
  abstrações sem necessidade clara.
