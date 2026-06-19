# Flyway — doc-flow

Índice local. **Documentação completa:** [`../../../../docs/MIGRATIONS.md`](../../../../docs/MIGRATIONS.md)

## Arquivos deste módulo

| Versão | Arquivo |
|---|---|
| V1–V3 | docflow — estrutura (tabelas, constraints, índices) |
| V4–V6 | seguranca — usuários |
| V7–V9 | rbac — catálogo + grupos |
| V10–V13 | seeds (rbac, docflow demo, usuários, vínculos) |

Release Orchestrator: `release-orchestrator/src/main/resources/db/migration/` (V14–V16).

## Reset

```bash
psql -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
./mvnw -pl application spring-boot:run
```
