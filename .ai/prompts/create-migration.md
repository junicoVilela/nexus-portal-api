# Prompt - Criar Migration Flyway

Crie uma migration Flyway para o módulo informado.

Leia antes: [`docs/MIGRATIONS.md`](../../docs/MIGRATIONS.md) e [`.ai/project/DATABASE_STANDARDS.md`](../project/DATABASE_STANDARDS.md).

## Regras

- PostgreSQL, snake_case, prefixo `tb_`, UUID como PK.
- **Não alterar** migration antiga já aplicada.
- Respeitar a **ordem de etapas** (ver abaixo).
- Numeração **global** — próxima versão após V16 = **V17**.

## Ordem ideal por módulo

1. **Schemas** — projeto usa `public`; módulo lógico no nome do arquivo (`docflow`, `seguranca`, `rbac`, `release_orchestrator`).
2. **Tabelas** — `01_tables` — só `CREATE TABLE` + `PRIMARY KEY`.
3. **Constraints** — `02_constraints` — `UNIQUE`, `FOREIGN KEY`, comentários.
4. **Índices** — `03_indexes`.
5. **Dados iniciais** — `04_seed_*` — `INSERT` com `ON CONFLICT`.
6. **Alterações incrementais** — `05_alter_*`, `06_alter_*`.
7. **Correções pontuais** — `07_fix_*`.

## Padrão de arquivo

```text
V{n}__{modulo}__{etapa}_{descricao}.sql
```

Exemplos:

```text
V17__release_orchestrator__05_alter_entrega_add_status.sql
V18__rbac__06_fix_grupo_editor_permissoes.sql
```

## Exemplo — nova tabela (etapas separadas)

**V17__meu_modulo__01_tables.sql**

```sql
CREATE TABLE tb_exemplo (
  id         UUID        NOT NULL,
  nome       VARCHAR(150) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_tb_exemplo PRIMARY KEY (id)
);
```

**V18__meu_modulo__02_constraints.sql**

```sql
ALTER TABLE tb_exemplo ADD CONSTRAINT uq_tb_exemplo_nome UNIQUE (nome);
```

**V19__meu_modulo__03_indexes.sql**

```sql
CREATE INDEX idx_tb_exemplo_nome ON tb_exemplo (nome);
```

## Dependências entre módulos

```text
docflow → seguranca (tb_usuario) → rbac (tb_grupo_usuario) → seeds → release_orchestrator
```

Não criar FK para tabela de módulo cuja migration ainda não rodou.
