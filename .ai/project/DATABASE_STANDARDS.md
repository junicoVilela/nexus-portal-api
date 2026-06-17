# Padrões de Banco de Dados

## Banco principal

PostgreSQL.

## Estratégia

Banco único, tabelas com prefixo `tb_` por módulo, no singular.

Exemplos reais do projeto:

```text
tb_cliente
tb_projeto
tb_modulo
tb_pagina
tb_pagina_revisao
tb_pagina_anexo
tb_publicacao
tb_cliente_projeto
tb_cliente_modulo
tb_cliente_pagina
tb_auditoria_evento
tb_usuario
tb_preview_token
tb_publicacao_changelog
tb_grupo
tb_grupo_permissao
tb_grupo_usuario

tb_produto_rh
tb_release
tb_release_item
tb_release_historico
tb_release_template
```

## IDs

UUID como chave primária em todas as tabelas:

```sql
id UUID PRIMARY KEY DEFAULT gen_random_uuid()
```

Quando o ID é gerado pelo Hibernate (`@GeneratedValue(strategy = GenerationType.UUID)`),
o `DEFAULT gen_random_uuid()` é opcional, mas mantemos por consistência em
tabelas com inserts manuais (seeds).

## Flyway

Migrations em:

```text
{modulo}/src/main/resources/db/migration
```

Numeração compartilhada entre módulos (V1, V2, V3… são únicos no classpath
agregado).

Padrão atual:

```text
doc-flow/src/main/resources/db/migration/V1__schema_dominio.sql
doc-flow/src/main/resources/db/migration/V2__schema_seguranca.sql
doc-flow/src/main/resources/db/migration/V3__schema_grupos.sql
release-orchestrator/src/main/resources/db/migration/V4__schema_release_orchestrator.sql
doc-flow/src/main/resources/db/migration/V5__seed_demonstracao.sql
```

## Regras para migrations

- Nunca alterar migration já aplicada em ambiente compartilhado.
- Criar nova migration para alteração futura.
- Nomes descritivos: `V{n}__o_que_faz.sql`.
- Separar migrations por módulo quando possível.
- Evitar scripts gigantes sem necessidade.

## Nomenclatura de tabelas

snake_case com prefixo `tb_`, **no singular**.

## Nomenclatura de colunas

snake_case.

Campos comuns:

```text
id
nome
titulo
descricao
status
created_at
updated_at
created_by
updated_by
```

Chaves estrangeiras seguem `{tabela_singular}_id`:

```text
produto_id
release_id
cliente_id
usuario_id
```

## Índices

Criar índices para:

- chaves estrangeiras
- campos usados em filtros frequentes
- campos únicos
- status usado em filtros recorrentes
- data de criação/atualização usada em ordenação/filtro

Nomenclatura: `idx_{tabela}_{coluna(s)}`.

## Constraints

Constraints com nomes claros, prefixadas pelo nome da tabela:

```text
pk_tb_release
fk_release_produto
uq_tb_release_produto_versao
idx_tb_release_status
idx_tb_release_updated
```

## Status

Enum no Java + `VARCHAR(30)` no banco:

```sql
status VARCHAR(30) NOT NULL DEFAULT 'RASCUNHO'
```

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 30)
private ReleaseStatus status;
```

## Datas e timestamps

Padrão do projeto:

- `TIMESTAMPTZ` (`TIMESTAMP WITH TIME ZONE`) para `created_at`/`updated_at`
  e demais timestamps.
- `DATE` para datas sem hora.

```sql
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
data_prevista DATE
```

Java correspondente:

```java
private OffsetDateTime createdAt;  // TIMESTAMPTZ
private LocalDate dataPrevista;    // DATE
```

Não misturar `timestamp without time zone` com `TIMESTAMPTZ`.

## Auditoria

Entidades de domínio principais carregam `created_at`, `updated_at`,
`created_by`, `updated_by`, preenchidos automaticamente pelo Spring Data
JPA Auditing através de `AuditableEntity` (ver `BACKEND_STANDARDS.md`).

Tabelas de evento/token/histórico que só precisam de `created_at` (+
`created_by` opcional) ficam livres dessa herança.
