# Jornadas de uso — Softon Portal

Narrativas **end-to-end** que complementam as specs por tela. Use este índice quando precisar entender *quem faz o quê, em qual ordem* — as specs em `release-orchestrator/` e `doc-flow/` detalham *campos, regras e API*.

> Última revisão: 2026-06-16.

## Como ler

1. Comece por [`00-cenario-feliz-acme.md`](00-cenario-feliz-acme.md) — fluxo integrado Release Orchestrator + DocFlow.
2. Para GitHub, Jenkins e cálculo de delta por cliente, leia [`01-github-jenkins-delta.md`](01-github-jenkins-delta.md) (índice com links para specs técnicas).

## Índice

| # | Documento | Conteúdo |
|---|---|---|
| 00 | [Cenário feliz — ACME v1.5.0](00-cenario-feliz-acme.md) | Jornada completa: release → entrega técnica → manual |
| 01 | [GitHub, Jenkins e delta](01-github-jenkins-delta.md) | Onde cada peça vive; MVP vs Fase 2; telas envolvidas |
| 02 | [Checklist por sprint](02-checklist-por-sprint.md) | Plano executável S0–S13+ com DoD por sprint |
| 03 | [Plano 90 dias + tarefas](03-plano-90-dias-e-tarefas.md) | Marcos fixos, calendário, backlog Jira/Linear S0–S7 |
| 04 | [Kickoff + prompts execução](04-kickoff-e-prompts-execucao.md) | One-pager + textos para pedir implementação no Cursor |

## Documentação relacionada

| Assunto | Onde |
|---|---|
| Specs por tela (Orchestrator) | [`../release-orchestrator/README.md`](../release-orchestrator/README.md) |
| Fluxo integrado (visão técnica) | [`../release-orchestrator/00-visao-geral-fluxo-integrado.md`](../release-orchestrator/00-visao-geral-fluxo-integrado.md) |
| Roadmap (fases 0–4) | [`../ROADMAP.md`](../ROADMAP.md) |
| DocFlow backend | [`../doc-flow/README.md`](../doc-flow/README.md) |
| DocFlow frontend | `softon-portal-web/docs/docflow/` |
| Release Orchestrator frontend | `softon-portal-web/docs/release-orchestrator/` |

## Legenda de estado (nas jornadas)

| Símbolo | Significado |
|---|---|
| ✅ | Implementado no portal (código existente) |
| 📋 | Especificado; UI ou integração ainda não no código |
| 🔧 | Trabalho nos repositórios de produto (fora do portal) |
