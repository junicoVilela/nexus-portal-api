# Flyway — migrations do portal

Convenção de migrations por **módulo lógico** e **etapa de DDL/DML**, com numeração **global única** num diretório único.

> Pasta física: `application/src/main/resources/db/migration/` (todas as migrations vivem juntas)  
> Padrão de nome: `V{n}__{modulo}__{etapa}_{descricao}.sql` (o módulo está só no nome do arquivo)

---

## Ordem ideal (dependências do banco)

Ao criar ou reorganizar migrations, respeite esta sequência **dentro de cada módulo**:

| Etapa | Sufixo no arquivo | Conteúdo |
|---|---|---|
| **1. Schemas** | *(implícito)* | Projeto usa schema PostgreSQL `public` + prefixo `tb_`. Módulos lógicos: `docflow`, `seguranca`, `rbac`, `release_orchestrator`. |
| **2. Tabelas** | `01_tables` | `CREATE TABLE` + `PRIMARY KEY` apenas |
| **3. Constraints** | `02_constraints` | `UNIQUE`, `FOREIGN KEY`, `CHECK`, comentários de coluna |
| **4. Índices** | `03_indexes` | `CREATE INDEX` |
| **5. Dados iniciais** | `04_seed_*` | `INSERT` idempotente (`ON CONFLICT`) |
| **6. Alterações incrementais** | `05_alter_*`, `06_alter_*`… | Novas colunas, tabelas, FKs em produção |
| **7. Correções pontuais** | `07_fix_*`, `08_fix_*`… | `UPDATE`/`DELETE` pontuais de dados |

**Nunca altere** migration já aplicada em ambiente compartilhado — crie nova versão na etapa 6 ou 7.

---

## Mapa atual (baseline — reset do zero)

Ordem global Flyway (V1 → V16):

| Versão | Módulo | Etapa | Arquivo |
|---|---|---|---|
| V1 | docflow | tabelas | `V1__docflow__01_tables.sql` |
| V2 | docflow | constraints | `V2__docflow__02_constraints.sql` |
| V3 | docflow | índices | `V3__docflow__03_indexes.sql` |
| V4 | seguranca | tabelas | `V4__seguranca__01_tables.sql` |
| V5 | seguranca | constraints | `V5__seguranca__02_constraints.sql` |
| V6 | seguranca | índices | `V6__seguranca__03_indexes.sql` |
| V7 | rbac | tabelas | `V7__rbac__01_tables.sql` |
| V8 | rbac | constraints | `V8__rbac__02_constraints.sql` |
| V9 | rbac | índices | `V9__rbac__03_indexes.sql` |
| V10 | rbac | seed | `V10__rbac__04_seed_catalog.sql` |
| V11 | docflow | seed | `V11__docflow__04_seed_demo.sql` |
| V12 | seguranca | seed | `V12__seguranca__04_seed_usuarios.sql` |
| V13 | rbac | seed | `V13__rbac__05_seed_usuarios_grupos.sql` |
| V14 | release_orchestrator | tabelas | `V14__release_orchestrator__01_tables.sql` |
| V15 | release_orchestrator | constraints | `V15__release_orchestrator__02_constraints.sql` |
| V16 | release_orchestrator | índices | `V16__release_orchestrator__03_indexes.sql` |

### Dependências entre módulos

```text
docflow (V1–V3)
    ↓
seguranca (V4–V6) — tb_usuario
    ↓
rbac (V7–V9) — tb_grupo_usuario → tb_usuario
    ↓
seeds: rbac catálogo (V10) → docflow demo (V11) → usuários (V12) → grupos (V13)
    ↓
release_orchestrator (V14–V16)
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

Credenciais seed: `admin/admin`, `editor/editor`, `revisor/revisor` (ver V12).

---

## Próximas migrations (exemplos)

| Necessidade | Nome sugerido |
|---|---|
| Nova coluna em release | `V17__release_orchestrator__05_alter_release_add_campo_x.sql` |
| Nova tabela de entrega | `V17__release_orchestrator__05_alter_entrega_tables.sql` (etapas 01–03 se módulo novo grande) |
| Corrigir seed | `V18__rbac__06_fix_permissao_orchestrator.sql` |

---

## Referências

- Padrões gerais: [`../../.ai/project/DATABASE_STANDARDS.md`](../../.ai/project/DATABASE_STANDARDS.md)
- Prompt criar migration: [`../../.ai/prompts/create-migration.md`](../../.ai/prompts/create-migration.md)
- DocFlow: [`../../docs/doc-flow/README.md`](../../docs/doc-flow/README.md)
