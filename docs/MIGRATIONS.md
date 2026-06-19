# Flyway — migrations do portal

Convenção de migrations por **módulo lógico** e **etapa de DDL/DML**, com numeração **global única** num diretório único.

> Pasta física: `application/src/main/resources/db/migration/` (todas as migrations vivem juntas)  
> Padrão de nome: `V{n}__{modulo}__{etapa}_{descricao}.sql` (o módulo está só no nome do arquivo)

---

## Tipos de migration

Baseline e seeds usam sufixos no nome do arquivo pra marcar a intenção. Não há etapas intermediárias (`02_*`, `03_*`) — tables + constraints + índices do mesmo módulo entram juntos em `01_schema`.

| Sufixo | Conteúdo |
|---|---|
| `01_schema` | Baseline do módulo: `CREATE TABLE` + constraints + índices |
| `04_seed_*` | Dados iniciais (`INSERT` idempotente com `ON CONFLICT`) |
| `05_alter_*` | Alterações incrementais pós-baseline |
| `06_fix_*` / `07_fix_*` | Correções pontuais de dados |

**Nunca altere** migration já aplicada em ambiente compartilhado — crie `05_alter_*` ou `06_fix_*` com o ajuste.

---

## Mapa atual (baseline — reset do zero)

Ordem global Flyway (V1 → V9):

| Versão | Módulo | Tipo | Arquivo |
|---|---|---|---|
| V1 | docflow | schema | `V1__docflow__01_schema.sql` |
| V2 | seguranca | schema | `V2__seguranca__01_schema.sql` |
| V3 | rbac | schema | `V3__rbac__01_schema.sql` |
| V4 | rbac | seed | `V4__rbac__04_seed_catalog.sql` |
| V5 | docflow | seed | `V5__docflow__04_seed_demo.sql` |
| V6 | seguranca | seed | `V6__seguranca__04_seed_usuarios.sql` |
| V7 | rbac | seed | `V7__rbac__04_seed_usuarios_grupos.sql` |
| V8 | release_orchestrator | schema | `V8__release_orchestrator__01_schema.sql` |
| V9 | release_orchestrator | alter | `V9__release_orchestrator__05_alter_modulo_produto.sql` |

### Dependências entre módulos

```text
docflow (V1)
    ↓
seguranca (V2) — tb_usuario
    ↓
rbac (V3) — tb_grupo_usuario → tb_usuario
    ↓
seeds: rbac catálogo (V4) → docflow demo (V5) → usuários (V6) → vínculos (V7)
    ↓
release_orchestrator (V8) + alter modulo_produto (V9)
```

---

## Tabelas por módulo

### docflow

`tb_cliente`, `tb_projeto`, `tb_modulo`, `tb_pagina`, `tb_pagina_revisao`, `tb_pagina_anexo`, `tb_publicacao`, `tb_cliente_*`, `tb_auditoria_evento`, `tb_preview_token`, `tb_publicacao_changelog`

### seguranca

`tb_usuario`

### rbac

`tb_dominio`, `tb_funcionalidade`, `tb_permissao`, `tb_grupo`, `tb_grupo_permissao`, `tb_grupo_usuario`

### release_orchestrator

`tb_produto_rh`, `tb_release`, `tb_release_item`, `tb_release_historico`, `tb_release_template`

---

## Reset local (PostgreSQL)

```bash
psql "$DB_URL" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
cd softon-portal-api && ./mvnw -pl application -am spring-boot:run
```

Credenciais seed: `admin/admin`, `editor/editor`, `revisor/revisor` (ver V6).

---

## Próximas migrations (exemplos)

| Necessidade | Nome sugerido |
|---|---|
| Nova coluna em release | `V10__release_orchestrator__05_alter_release_add_campo_x.sql` |
| Nova tabela em módulo existente | `V10__release_orchestrator__05_alter_entrega_tables.sql` |
| Corrigir seed | `V10__rbac__06_fix_permissao_orchestrator.sql` |
| Novo módulo grande | `V10__novo_modulo__01_schema.sql` (baseline) + V11 seed se precisar |

---

## Referências

- Padrões gerais: [`../../.ai/project/DATABASE_STANDARDS.md`](../../.ai/project/DATABASE_STANDARDS.md)
- Prompt criar migration: [`../../.ai/prompts/create-migration.md`](../../.ai/prompts/create-migration.md)
- DocFlow: [`../../docs/doc-flow/README.md`](../../docs/doc-flow/README.md)
