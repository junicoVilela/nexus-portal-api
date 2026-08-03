# AGENTS.md - Nexus Portal API

Backend do **Nexus Portal API** — monólito modular simples em Java 21 / Spring Boot 4.x / PostgreSQL.

## Padrões e arquitetura

Toda a documentação de padrões está centralizada em `.ai/`. Consulte antes de gerar código:

- `.ai/project/PROJECT_CONTEXT.md` — objetivo, stack, módulos
- `.ai/project/ARCHITECTURE.md` — estrutura Maven, pacotes, camadas, responsabilidades
- `.ai/project/BACKEND_STANDARDS.md` — nomenclatura, Lombok, MapStruct, UUID, Interface+Impl
- `.ai/project/JAVA_STYLE.md` — estilo Java moderno: Streams, Lambdas, Optional, Records, var, Collections, clean code
- `.ai/project/API_STANDARDS.md` — REST, CRUD, status HTTP, paginação
- `.ai/project/DATABASE_STANDARDS.md` — tabelas `tb_*`, Flyway, TIMESTAMPTZ
- `.ai/project/SECURITY_STANDARDS.md` — JWT, autorização, auditoria
- `.ai/project/TEST_STANDARDS.md` — JUnit 5, Mockito, AssertJ

## Módulos

| Módulo | Status | Spec |
|--------|--------|------|
| Gateway | Implementado | `.ai/modules/gateway.md` |
| DocFlow | Implementado | `.ai/modules/docflow.md` |
| Release Orchestrator | Implementado | `.ai/modules/releases.md` → `docs/release-orchestrator/` |
| Release Orchestrator | Previsto | `docs/release-orchestrator/` |
| Sistemas | Previsto | `.ai/modules/sistemas.md` |
| Access Control | Previsto | `.ai/modules/accesscontrol.md` |
| Usuários | Previsto | `.ai/modules/usuarios.md` |
| Auditoria | Previsto | `.ai/modules/auditoria.md` |
| Monitoramento | Previsto | `.ai/modules/monitoramento.md` |
| Notificações | Previsto | `.ai/modules/notificacoes.md` |
| Configurações | Previsto | `.ai/modules/configuracoes.md` |

## Prompts reutilizáveis

- `.ai/prompts/create-module.md`
- `.ai/prompts/create-crud.md`
- `.ai/prompts/create-endpoint.md`
- `.ai/prompts/create-migration.md`
- `.ai/prompts/create-tests.md`
- `.ai/prompts/review-code.md`

## Exemplo de estrutura

- `.ai/examples/backend-module-structure.txt`
