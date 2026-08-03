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

## Recriar banco local

Com o Postgres do compose (`infra/docker`):

```bash
docker exec nexus-postgres psql -U nexus_platform -d postgres -c "DROP DATABASE IF EXISTS nexus_platform;"
docker exec nexus-postgres psql -U nexus_platform -d postgres -c "CREATE DATABASE nexus_platform OWNER nexus_platform;"
./mvnw -pl application -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Defaults do perfil `dev` (`application-dev.yml`):

- DB: `nexus_platform`
- user/password: `nexus_platform` / `nexus!@#`

## Seeds padrão

- Usuários: `admin`, `editor`, `revisor`, `leitor` (senhas via `docflow.security.*-password` no boot / hashes no seed)
- 20 templates de página (códigos neutros: `LAB_FILTROS`, `PAINEL_METRICAS`, …)
- Manual DocFlow demo (cliente/projeto/páginas) em V12 — opcional para showcase
