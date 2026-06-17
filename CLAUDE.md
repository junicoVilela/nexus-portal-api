# CLAUDE.md - Softon Portal API

Backend do **Softon Portal API** — monólito modular simples em Java 21 / Spring Boot 4.x / PostgreSQL.

---

## Padrões e arquitetura

Toda a documentação de padrões está centralizada em `.ai/`. Consulte antes de gerar, revisar ou refatorar código:

| Arquivo | Conteúdo |
|---------|----------|
| `.ai/project/PROJECT_CONTEXT.md` | Objetivo, stack, módulos atuais e previstos |
| `.ai/project/ARCHITECTURE.md` | Estrutura Maven, pacotes, camadas, responsabilidades, comunicação entre módulos |
| `.ai/project/BACKEND_STANDARDS.md` | Nomenclatura, Controller/Service/Repository/Entity/DTO/Mapper, Lombok, MapStruct, UUID |
| `.ai/project/JAVA_STYLE.md` | Estilo Java moderno: Streams, Lambdas, Optional, Records, var, Collections, clean code |
| `.ai/project/API_STANDARDS.md` | Prefixos REST, CRUD, status HTTP, paginação, versionamento |
| `.ai/project/DATABASE_STANDARDS.md` | Tabelas `tb_*`, Flyway, colunas, índices, constraints, TIMESTAMPTZ |
| `.ai/project/SECURITY_STANDARDS.md` | Autenticação JWT, autorização, auditoria, CORS |
| `.ai/project/TEST_STANDARDS.md` | JUnit 5, Mockito, AssertJ, organização, o que testar |

## Specs por módulo

| Arquivo | Módulo |
|---------|--------|
| `.ai/modules/docflow.md` | DocFlow (Clientes, Projetos, Módulos, Páginas, Publicações) |
| `.ai/modules/releases.md` | Release Orchestrator (aponta para `docs/release-orchestrator/`) |
| `.ai/modules/gateway.md` | Gateway |

## Documentação funcional

| Diretório | Conteúdo |
|-----------|----------|
| `docs/release-orchestrator/` | Spec técnica do Release Orchestrator: modelo de dados, endpoints, regras de negócio, DTOs, testes, evoluções MVP |
| `docs/release-orchestrator/` | Spec funcional do Release Orchestrator: telas, fluxo integrado, modelo de dados, decisões técnicas |

## Prompts e exemplos reutilizáveis

| Diretório | Conteúdo |
|-----------|----------|
| `.ai/prompts/` | Prompts para criar módulo, CRUD, endpoint, migration, testes, review |
| `.ai/examples/` | Exemplo de estrutura de módulo backend |
