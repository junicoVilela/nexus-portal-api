# Prompt - Criar Novo Módulo Backend

Crie um novo módulo para o projeto Softon Portal API.

## Regras obrigatórias

- Usar monólito modular simples.
- Não usar arquitetura hexagonal.
- Não criar ports/adapters.
- Não criar use cases para CRUD simples.
- Usar Controller -> Service -> Repository.
- **Não usar `modules` no caminho do pacote.**

## Localização

Cada módulo de negócio vive em seu próprio sub-pacote direto sob `br.com.softon.portal`:

```text
src/main/java/br/com/softon/portal/{nomeModulo}/
```

Exemplo para um módulo `sistemas`:

```text
doc-flow/src/main/java/br/com/softon/portal/sistemas/
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
br.com.softon.portal.{nomeModulo}.controller
br.com.softon.portal.{nomeModulo}.service
br.com.softon.portal.{nomeModulo}.repository
br.com.softon.portal.{nomeModulo}.entity
br.com.softon.portal.{nomeModulo}.dto.request
br.com.softon.portal.{nomeModulo}.dto.response
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
package br.com.softon.portal.sistemas.controller;

// ERRADO
package br.com.softon.portal.modules.sistemas.controller;
```
