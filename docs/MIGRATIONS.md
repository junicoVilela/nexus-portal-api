# Migrations (Flyway) — Nexus Platform

Greenfield: banco do zero. Pasta física única:

`application/src/main/resources/db/migration/`

## Organização

Por **módulo** e **camada** (sem sequences SERIAL — IDs são UUID):

| Versão | Módulo | Camada |
|--------|--------|--------|
| V1 | platform | extensions |
| V2 | identity-access | tables |
| V3 | identity-access | constraints & comments |
| V4 | identity-access | indexes |
| V5 | identity-access | seed catálogo RBAC |
| V6 | identity-access | seed usuários + política de senha |
| V7 | docflow | tables |
| V8 | docflow | constraints & comments |
| V9 | docflow | indexes |
| V10 | docflow | seed templates (20 modelos + versões) |
| V11 | docflow | seed ajuda interativa |
| V12 | docflow | seed manual demo |
| V13 | release-orchestrator | tables |
| V14 | release-orchestrator | constraints & comments |
| V15 | release-orchestrator | indexes |
| V16 | ai | tables sessão + mensagem (`nexus-ai`) |
| V17 | ai | jobs + propostas (`nexus-ai`) |
| V18–V24 | ai | page spec, jobs, importação de documento, componentes |
| V25 | release-orchestrator | hosts de execução (`tb_host_orchestrator`) + RBAC `HOST` |
| V26 | release-orchestrator | instalações do cliente (`tb_instalacao_cliente`) + RBAC `INSTALACAO` |
| V27 | release-orchestrator | config da instalação + reserva de portas (RF-004 / RF-005) |
| V28 | release-orchestrator | health da instalação + alvos da entrega (`tb_entrega_instalacao`) |
| V29 | release-orchestrator | manifesto de implantação + histórico de deploy (dry-run) |
| V34 | docflow | robustez da publicação (relatório em `text`, cancelamento, índice do watchdog) |
| V35 | docflow | `search_vector` como coluna gerada + índice GIN em uso pela busca |
| V36 | docflow | revisor/prazo na página + `tb_pagina_snippet` |
| V37 | docflow | ação RBAC `PAGINA:APROVAR` (ADMIN, EDITOR, REVISOR) |

## Recriar banco local

Com o Postgres do compose (`infra/docker`):

```bash
docker exec nexus-platform-postgres psql -U nexus_platform -d postgres -c "DROP DATABASE IF EXISTS nexus_platform;"
docker exec nexus-platform-postgres psql -U nexus_platform -d postgres -c "CREATE DATABASE nexus_platform OWNER nexus_platform;"
./mvnw -pl application -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Defaults do perfil `dev` (`application-dev.yml`):

- DB: `nexus_platform`
- user/password: `nexus_platform` / `nexus!@#`

## Seeds padrão

- Usuários: `admin`, `editor`, `revisor`, `leitor` (senhas via `docflow.security.*-password` no boot / hashes no seed)
- 20 templates de página (códigos neutros: `LAB_FILTROS`, `PAINEL_METRICAS`, …)
- Manual DocFlow demo (cliente/projeto/páginas) em V12 — opcional para showcase
