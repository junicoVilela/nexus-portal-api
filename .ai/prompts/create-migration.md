# Prompt - Criar Migration Flyway

Crie uma migration Flyway para o módulo informado.

Leia antes: [`docs/MIGRATIONS.md`](../../docs/MIGRATIONS.md) e [`.ai/project/DATABASE_STANDARDS.md`](../project/DATABASE_STANDARDS.md).

## Regras

- PostgreSQL, snake_case, prefixo `tb_`, UUID como PK.
- **Não alterar** migration antiga já aplicada.
- Numeração **global** — próxima versão após V9 = **V10**.
- Migrations vivem em `application/src/main/resources/db/migration/`.

## Tipos de migration

| Sufixo | Quando usar |
|---|---|
| `01_schema` | Baseline de módulo novo: tabelas + constraints + índices num arquivo só |
| `04_seed_*` | Dados iniciais (`INSERT` idempotente com `ON CONFLICT`) |
| `05_alter_*` | Mudança pós-baseline: nova coluna, nova tabela, nova FK |
| `06_fix_*` / `07_fix_*` | Correção pontual de dados |

## Padrão de arquivo

```text
V{n}__{modulo}__{tipo}_{descricao}.sql
```

Exemplos:

```text
V10__release_orchestrator__05_alter_entrega_add_status.sql
V10__rbac__06_fix_grupo_editor_permissoes.sql
V10__novo_modulo__01_schema.sql
```

## Exemplo — módulo novo

**V10__meu_modulo__01_schema.sql**

```sql
CREATE TABLE tb_exemplo (
  id         UUID         NOT NULL DEFAULT gen_random_uuid(),
  nome       VARCHAR(150) NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT pk_tb_exemplo PRIMARY KEY (id)
);

ALTER TABLE tb_exemplo ADD CONSTRAINT uq_tb_exemplo_nome UNIQUE (nome);

CREATE INDEX idx_tb_exemplo_nome ON tb_exemplo (nome);
```

## Dependências entre módulos

```text
docflow → seguranca (tb_usuario) → rbac (tb_grupo_usuario) → seeds → release_orchestrator
```

Não criar FK para tabela de módulo cuja migration ainda não rodou.
