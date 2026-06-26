# Flyway — migrations do portal

Convenção de migrations por **módulo lógico** e **etapa**, com numeração **global única** num diretório único.

> Pasta física: `application/src/main/resources/db/migration/` (todas as migrations vivem juntas)
> Padrão de nome: `V{n}__{modulo}__{etapa}_{descricao}.sql` (o módulo está só no nome do arquivo)

---

## Princípios

1. **Baseline consolidado, sem ALTER pós-baseline.** Enquanto não houver primeiro deploy compartilhado, qualquer mudança de schema entra direto no `01_schema` do módulo (colunas novas, constraints, índices). Não usar `05_alter_*` nessa fase — reorganizar a baseline em vez de empilhar alters.
2. **Constraints declaradas inline no `CREATE TABLE`** (PK, UNIQUE, FK, CHECK). Nada de `ALTER TABLE ... ADD CONSTRAINT` separado.
3. **Política de seeds enxuta:** só o módulo de segurança tem seeds aplicados (catálogo RBAC mínimo + usuários + vínculos). Seeds de outros módulos entram sob demanda.
4. **Idempotência nos seeds:** todo `INSERT` em migration de seed usa `ON CONFLICT … DO UPDATE/NOTHING`, pra suportar reaplicação.

Quando houver primeiro deploy compartilhado, a partir daí ALTER vira obrigatório: a baseline é "congelada" e as mudanças passam a entrar como `05_alter_*` ou `06_fix_*`.

---

## Tipos de migration

| Sufixo | Conteúdo |
|---|---|
| `01_schema` | Baseline do módulo: `CREATE TABLE` com **constraints + índices** completos |
| `04_seed_*` | Dados iniciais (`INSERT` idempotente) |
| `05_alter_*` | Alterações incrementais **após o primeiro deploy compartilhado** |
| `06_fix_*` / `07_fix_*` | Correções pontuais de dados (idem) |

---

## Mapa atual (baseline limpo)

| Versão | Módulo | Tipo | Arquivo |
|---|---|---|---|
| V1 | docflow | schema | `V1__docflow__01_schema.sql` |
| V2 | seguranca | schema | `V2__seguranca__01_schema.sql` |
| V3 | rbac | schema | `V3__rbac__01_schema.sql` |
| V4 | release_orchestrator | schema | `V4__release_orchestrator__01_schema.sql` (consolida F0–F4 — GitHub, Jenkins, FTP/SFTP, BUCKET, retry, build status) |
| V5 | rbac | seed | `V5__rbac__04_seed_catalog.sql` — só SEGURANCA + SISTEMA |
| V6 | seguranca | seed | `V6__seguranca__04_seed_usuarios.sql` — admin/editor/revisor/leitor |
| V7 | rbac | seed | `V7__rbac__04_seed_usuarios_grupos.sql` |

### Dependências entre módulos

```text
docflow (V1)               — independente
seguranca (V2)             — tb_usuario
rbac (V3)                  — tb_grupo_usuario → tb_usuario (V2)
release_orchestrator (V4)  — independente das outras (responsavel_id é UUID sem FK)
seeds:
  rbac catálogo (V5)       — usa tb_dominio/funcionalidade/permissao/grupo de V3
  seguranca usuarios (V6)  — usa tb_usuario de V2
  rbac vínculos (V7)       — usa tb_grupo (V5) + tb_usuario (V6)
```

---

## Tabelas por módulo

### docflow

`tb_cliente`, `tb_projeto`, `tb_modulo`, `tb_pagina`, `tb_pagina_revisao`, `tb_pagina_anexo`, `tb_publicacao`, `tb_cliente_projeto`, `tb_cliente_modulo`, `tb_cliente_pagina`, `tb_auditoria_evento`, `tb_preview_token`, `tb_publicacao_changelog`

### seguranca

`tb_usuario`

### rbac

`tb_dominio`, `tb_funcionalidade`, `tb_permissao`, `tb_grupo`, `tb_grupo_permissao`, `tb_grupo_usuario`

### release_orchestrator

`tb_produto_rh`, `tb_release`, `tb_release_item`, `tb_release_historico`, `tb_release_template`, `tb_modulo_produto`, `tb_release_modulo_versao`, `tb_artefato_release_modulo`, `tb_cliente_orchestrator`, `tb_contato_orchestrator`, `tb_config_entrega_orchestrator`, `tb_dominio_produto`, `tb_funcionalidade_produto`, `tb_cliente_funcionalidade_produto`, `tb_cliente_produto`, `tb_cliente_produto_modulo`, `tb_proxima_entrega`, `tb_entrega`, `tb_entrega_modulo`, `tb_entrega_modulo_artefato`

---

## Reset local (PostgreSQL)

```bash
psql "$DB_URL" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
cd softon-portal-api && ./mvnw -pl application -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Credenciais seed (V6): `admin/admin`, `editor/editor`, `revisor/revisor`, `leitor/leitor`.

Como o catálogo RBAC só cobre SEGURANCA + SISTEMA, **EDITOR e REVISOR ficam sem permissões** até criar `V8__rbac__04_seed_{modulo}.sql` para os módulos correspondentes.

---

## Pegadinha: JAR stale no `~/.m2/`

Os módulos `doc-flow/` e `release-orchestrator/` **não publicam migrations** — todas vivem em `application/`. Se um JAR antigo desses módulos sobreviver em `~/.m2/repository/br/com/softon/`, ele pode reintroduzir migrations duplicadas e o boot quebra com `FlywayException: Found more than one migration with version N`.

```bash
# fix:
rm -rf ~/.m2/repository/br/com/softon
./mvnw clean install -DskipTests
```

---

## Próximas migrations (exemplos)

> Estamos pré-deploy compartilhado. Mudanças em tabelas existentes devem **alterar o `01_schema` do módulo**, não criar `05_alter_*`.

| Necessidade | Onde |
|---|---|
| Nova coluna em `tb_produto_rh` | direto em `V4__release_orchestrator__01_schema.sql` |
| Nova tabela do release_orchestrator | direto em `V4__release_orchestrator__01_schema.sql` |
| Cat. RBAC do DocFlow + permissões pra EDITOR/REVISOR | `V8__rbac__04_seed_docflow.sql` |
| Cat. RBAC do Release Orchestrator | `V9__rbac__04_seed_release_orchestrator.sql` |
| Dados demo do DocFlow (sob demanda) | `V8__docflow__04_seed_demo.sql` (escolher próxima livre) |
| Novo módulo grande | `V8__novo_modulo__01_schema.sql` + V9 seed se precisar |

Após primeiro deploy real, retomar `05_alter_*` / `06_fix_*` em vez de mexer no baseline.

---

## Referências

- Padrões gerais: [`../../.ai/project/DATABASE_STANDARDS.md`](../../.ai/project/DATABASE_STANDARDS.md)
- Prompt criar migration: [`../../.ai/prompts/create-migration.md`](../../.ai/prompts/create-migration.md)
- DocFlow: [`../../docs/doc-flow/README.md`](../../docs/doc-flow/README.md)
