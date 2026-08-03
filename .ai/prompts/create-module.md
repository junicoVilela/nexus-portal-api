# Prompt - Criar Novo Módulo Backend

Crie um novo módulo para o projeto Nexus Portal API.

## Regras obrigatórias

- Usar monólito modular simples.
- Não usar arquitetura hexagonal.
- Não criar ports/adapters.
- Não criar use cases para CRUD simples.
- Usar Controller -> Service -> Repository.
- **Não usar `modules` no caminho do pacote.**

## Localização

Cada módulo de negócio vive em seu próprio sub-pacote direto sob `com.nexus.portal`:

```text
src/main/java/br/com/nexus/portal/{nomeModulo}/
```

Exemplo para um módulo `sistemas`:

```text
doc-flow/src/main/java/br/com/nexus/portal/sistemas/
```

## Estrutura obrigatória de pastas

```text
{nomeModulo}/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
│   ├── request/
│   └── response/
├── mapper/       (adicionar quando necessário)
└── exception/    (adicionar quando necessário)
```

## Pacotes Java

```text
com.nexus.portal.{nomeModulo}.controller
com.nexus.portal.{nomeModulo}.service
com.nexus.portal.{nomeModulo}.repository
com.nexus.portal.{nomeModulo}.entity
com.nexus.portal.{nomeModulo}.dto.request
com.nexus.portal.{nomeModulo}.dto.response
```

## Gerar

- Entity JPA (com `extends AuditableEntity` quando necessário)
- Repository Spring Data JPA
- Service com regras de negócio
- Controller REST com `@RequestMapping("/api/v1/{recursos}")` — **cada
  endpoint anotado com `@PreAuthorize(Permissoes.X_Y)`** granular.
- Request DTOs (record com Bean Validation)
- Response DTOs (record simples)
- Exception específica em `exception/` (quando necessário)
- Migration Flyway em `src/main/resources/db/migration/`:
  - DDL da tabela `tb_{recurso}`.
  - **Seed RBAC**: incluir a funcionalidade em `tb_funcionalidade`,
    permissões CRUD em `tb_permissao`, e vínculos em `tb_grupo_permissao`
    para ADMIN/EDITOR/LEITOR/REVISOR. Seguir o padrão de V8/V9.
- Constantes em `shared/security/Permissoes.java` para cada permissão da
  nova funcionalidade.
- Teste unitário do Service para regras de negócio

## Padrões

- Java 21
- Spring Boot 4.x
- DTOs com `record` quando possível
- Bean Validation nos requests
- Não expor Entity na API
- Endpoints com `/api/v1`
- Nomes de tabelas: `tb_{recurso}`

## Exemplo de pacote correto

```java
// CORRETO
package com.nexus.portal.sistemas.controller;

// ERRADO
package com.nexus.portal.modules.sistemas.controller;
```
