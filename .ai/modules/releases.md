# Módulo Release Orchestrator

## Objetivo

Gerenciar versões, release notes e histórico de alterações dos sistemas internos.

## Status

Implementado no módulo Maven `release-orchestrator`.

## Especificação técnica completa

A spec detalhada do Release Orchestrator (arquitetura, modelo de dados, endpoints, regras de negócio, DTOs, segurança, testes e evoluções MVP) está em:

```text
docs/release-orchestrator/
```

Consulte antes de implementar, corrigir ou refatorar código neste módulo:

- `README.md` — visão geral e índice.
- `02-modelo-dados.md` — entidades JPA, enums, relacionamentos, migration SQL.
- `03-api-endpoints.md` — todos os endpoints REST com contratos request/response.
- `04-regras-negocio.md` — fluxo de status, validações, restrições de edição/exclusão.
- `05-dtos.md` — todos os records de request e response.
- `08-fase0-evolucoes.md` — ModuloProduto, ArtefatoReleaseModulo, PDF renderer.

## Escopo funcional integrado

O Release Orchestrator trabalha junto com o futuro Release Orchestrator. A visão geral e fronteira entre os dois módulos está em:

```text
docs/release-orchestrator/00-visao-geral-fluxo-integrado.md
```

## Localização Maven

```text
release-orchestrator/src/main/java/br/com/softon/portal/releaseorchestrator/
```

## Pacote base

```text
br.com.softon.portal.releaseorchestrator
```
