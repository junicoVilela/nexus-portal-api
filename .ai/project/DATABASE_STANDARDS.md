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
tb_dominio
tb_funcionalidade
tb_permissao
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

Migrations em **um único diretório** no módulo `application`:

```text
application/src/main/resources/db/migration/
```

Mesmo que o nome do arquivo carregue o módulo lógico (`V{n}__{modulo}__...`),
o banco é único (schema `public`) e a co-localização física simplifica
revisão e numeração. A convenção `{modulo}` continua no nome do arquivo.

**Documentação completa:** [`docs/MIGRATIONS.md`](../../docs/MIGRATIONS.md)

### Convenção de nome

```text
V{n}__{modulo}__{etapa}_{descricao}.sql
```

Etapas por módulo (ordem de dependências):

| Etapa | Sufixo | Conteúdo |
|---|---|---|
| 1 | *(implícito)* | Schema `public` + módulo lógico |
| 2 | `01_tables` | Tabelas + PK |
| 3 | `02_constraints` | UNIQUE, FK, CHECK |
| 4 | `03_indexes` | Índices |
| 5 | `04_seed_*` | Dados iniciais |
| 6 | `05_alter_*` | Alterações incrementais |
| 7 | `06_fix_*` / `07_fix_*` | Correções pontuais de dados |

### Baseline atual (V1–V16)

```text
V1–V3   docflow              (tabelas, constraints, índices)
V4–V6   seguranca            (tb_usuario)
V7–V9   rbac                 (catálogo + grupos)
V10–V13 seeds                (rbac, docflow demo, usuários, vínculos)
V14–V16 release_orchestrator (tabelas, constraints, índices)
```

## Regras para migrations

- Nunca alterar migration já aplicada em ambiente compartilhado.
- Criar nova migration na etapa 6 (alter) ou 7 (fix).
- Separar por módulo e por etapa (tabelas / constraints / índices / seed).
- Nomes descritivos: `V17__release_orchestrator__05_alter_entrega_status.sql`.
- Evitar scripts gigantes que misturam etapas.

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
