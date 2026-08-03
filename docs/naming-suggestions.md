# Naming — Sugestões de Padronização

Documento de sugestão, **não aplicado em código** (decisão pendente). Lista os pontos de drift de nomenclatura encontrados entre código backend, frontend, banco e specs, e propõe um conjunto coerente.

> Última revisão: 2026-05-31. Aplicável tanto a `nexus-portal-api` quanto a `nexus-portal-web`.

---

## 1. `ProdutoRh` → `Produto`

### Contexto
A entidade Java se chama `ProdutoRh` (vestígio de "Recursos Humanos" do escopo original). A tabela é `produtos_rh`. O Release Orchestrator (spec) já documenta a intenção: *"renomear conceitual para `Produto` ao expor via API"*.

### Estado atual

| Camada | Identificador | Onde |
|---|---|---|
| Entity | `ProdutoRh` | `release-orchestrator/.../entity/ProdutoRh.java` |
| Repository | `ProdutoRhRepository` | `release-orchestrator/.../repository/` |
| Service | `ProdutoRhService` | `release-orchestrator/.../service/` |
| Controller | `ProdutoRhController` | mapeado em `/api/v1/release-orchestrator/produtos` |
| DTO request | `ProdutoRhRequest` | dto/request/ |
| DTO response | `ProdutoRhResponse` | dto/response/ |
| Tabela | `produtos_rh` | V4 migration |
| Frontend model | `ProdutoRh` | `modules/release-orchestrator/models/produto-rh.model.ts` |
| Frontend service | `ProdutoRhService` | `modules/release-orchestrator/services/produto-rh.service.ts` |

A URL pública (`/produtos`) e o nome conceitual já usam `Produto`. Só os identificadores internos ainda carregam `Rh`.

### Proposta

| Antes | Depois |
|---|---|
| `ProdutoRh` (entity) | `Produto` |
| `ProdutoRhRepository` | `ProdutoRepository` |
| `ProdutoRhService` | `ProdutoService` |
| `ProdutoRhController` | `ProdutoController` |
| `ProdutoRhRequest` / `ProdutoRhResponse` | `ProdutoRequest` / `ProdutoResponse` |
| `AlterarStatusProdutoRequest` | (mantém — já está sem `Rh`) |
| Tabela `produtos_rh` | `produtos` |
| Frontend `ProdutoRh` (model) | `Produto` |
| Frontend `ProdutoRhService` | `ProdutoService` |
| Frontend arquivo `produto-rh.model.ts` | `produto.model.ts` |
| Frontend arquivo `produto-rh.service.ts` | `produto.service.ts` |

### Impacto técnico
- **Backend**: rename refactor em IDE + migration de tabela (`ALTER TABLE produtos_rh RENAME TO produtos`). FKs continuam funcionando.
- **Frontend**: rename de classes/arquivos + ajuste de imports.
- **Specs**: substituir `ProdutoRh` → `Produto`, `produtos_rh` → `produtos` em todas as docs.
- **Cuidado**: a constraint name `uq_releases_produto_versao` já não tem `rh` — manter.

### Custo
Baixo. Refactor mecânico de IDE + 1 migration Flyway.

### Recomendação
**Aplicar.** O nome `Rh` confunde quem entra novo no projeto e contradiz o restante da spec, que já usa `Produto`.

---

## 2. Prefixo `Rh*` / `Hub*` no frontend

### Estado atual

Vários componentes e a constante de rotas do release-orchestrator usam prefixos que ecoam o nome antigo:

| Identificador | Arquivo |
|---|---|
| `RhDashboardComponent` | `pages/dashboard/rh-dashboard.component.ts` |
| `RhProdutosComponent` | `pages/produtos/rh-produtos.component.ts` |
| `RhTemplatesComponent` | `pages/templates/rh-templates.component.ts` |
| `RhGuiaComponent` | `pages/guia/rh-guia.component.ts` |
| `ReleaseHubShellComponent` | `shell/release-orchestrator-shell.component.ts` |
| `RELEASE_ORCHESTRATOR_ROUTES` | `release-orchestrator.routes.ts` |

A pasta do módulo já é `release-orchestrator/` e o nome dos arquivos `*.component.ts` já é `release-orchestrator-shell.component.ts` — mas a **classe** dentro do arquivo se chama `ReleaseHubShellComponent`. Mistura forte.

### Proposta

| Antes | Depois |
|---|---|
| `RhDashboardComponent` | `ReleaseOrchestratorDashboardComponent` ou `DashboardComponent` (já em escopo do módulo) |
| `RhProdutosComponent` | `ProdutosComponent` |
| `RhTemplatesComponent` | `TemplatesComponent` |
| `RhGuiaComponent` | `GuiaComponent` |
| `ReleaseHubShellComponent` | `ReleaseOrchestratorShellComponent` |
| `RELEASE_ORCHESTRATOR_ROUTES` | `RELEASE_ORCHESTRATOR_ROUTES` |
| Arquivo `rh-dashboard.component.ts` | `dashboard.component.ts` |
| Arquivo `rh-produtos.component.ts` | `produtos.component.ts` |
| Arquivo `rh-templates.component.ts` | `templates.component.ts` |
| Arquivo `rh-guia.component.ts` | `guia.component.ts` |

### Argumentos
- Os componentes vivem dentro de `modules/release-orchestrator/`, então o prefixo `Rh` ou `Hub` no nome é redundante.
- Angular convencionalmente não prefixa com o nome do módulo (esse já é o pacote).
- "Hub" não corresponde a nenhum conceito de negócio definido — é um termo herdado da UI antiga.

### Custo
Médio. Renomear classes/arquivos + atualizar imports + atualizar `loadComponent` nas rotas.

### Recomendação
**Aplicar.** Reduz ruído e alinha com a pasta do módulo.

---

## 3. Convenção `tb_*` em tabelas (DATABASE_STANDARDS vs realidade)

### Estado atual

`nexus-portal-api/.ai/project/DATABASE_STANDARDS.md` diz:
> Tabelas com prefixo `tb_{recurso}`.

Mas as tabelas reais não seguem:

| Tabela real | Esperado pela convenção |
|---|---|
| `produtos_rh` | `tb_produtos_rh` |
| `releases` | `tb_releases` |
| `release_itens` | `tb_release_itens` |
| `release_historico` | `tb_release_historico` |
| `release_templates` | `tb_release_templates` |

### Opções

**A. Remover o `tb_` da convenção (alinhar standards com o código).**
- Custo: edit em 1 arquivo `DATABASE_STANDARDS.md`.
- Pró: o código continua intocado, e o estilo "tabelas sem prefixo" é o que muitos projetos modernos usam (Spring, Rails, Hibernate convencionam sem prefixo).
- Contra: perde a convenção declarada (se era intencional para identificar tabelas).

**B. Adicionar `tb_` no código (alinhar código com a convenção).**
- Custo: migration grande renomeando 5 tabelas + ajuste de `@Table(name=...)` em 5 entities.
- Pró: respeita o que foi documentado como padrão.
- Contra: trabalho mecânico sem ganho real de leitura/manutenção.

### Recomendação
**Opção A.** O código está consistente sem `tb_`. Atualizar `DATABASE_STANDARDS.md` para refletir essa convenção (sem prefixo), explicando que o prefixo `tb_` foi descartado.

---

## 4. Caminho de rota no frontend

A rota do release-orchestrator no Angular usa a constante `RELEASE_ORCHESTRATOR_ROUTES`. Sem ter visto onde ela é registrada (arquivo `app.routes.ts` ou similar), provavelmente o caminho público é `/release-hub` ou `/release-orchestrator`.

### Recomendação
- Caminho público: **`/release-orchestrator`** (consistente com o módulo Maven, pasta, prefixo de API).
- Constante: `RELEASE_ORCHESTRATOR_ROUTES`.

---

## 5. Nome do módulo Maven `release-orchestrator` vs pacote `releaseorchestrator`

### Estado atual
- Módulo Maven: `release-orchestrator` (com hífen)
- Pacote Java: `com.nexus.portal.releaseorchestrator` (sem hífen — Java não permite)

Isso **não é drift, é necessidade técnica**. Pacotes Java não aceitam hífen. Manter como está.

---

## 6. Resumo executivo

| Item | Aplicar? | Custo | Quem afetado |
|---|---|---|---|
| `ProdutoRh` → `Produto` (todas as camadas + tabela) | **Sim** | Baixo | Back + Front + Banco |
| Remover prefixo `Rh*`/`Hub*` no frontend | **Sim** | Médio | Front |
| Atualizar `DATABASE_STANDARDS.md` (remover `tb_`) | **Sim** | Trivial | Docs |
| Rota `/release-orchestrator` | Verificar e ajustar se aplicável | Baixo | Front |
| Manter `release-orchestrator` vs `releaseorchestrator` (Maven/Java) | Sem ação | — | — |

### Sequência sugerida (se aprovado)
1. Atualizar `DATABASE_STANDARDS.md` (opção A acima).
2. Renomear backend: `ProdutoRh*` → `Produto*` + migration de tabela.
3. Renomear frontend: classes `Rh*`/`Hub*` + arquivos.
4. Atualizar specs (release-orchestrator + orchestrator) que mencionam `ProdutoRh`.
5. Atualizar `auth.md`, `usuarios.md` e outras specs já corrigidas que ainda mencionam o nome antigo.

Tudo em PRs separados para revisão isolada.
