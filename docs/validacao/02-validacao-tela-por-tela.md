# 02 — Validação tela por tela

Para cada tela do Release Orchestrator: caminho, objetivo, campos, comportamento esperado, persistência e critérios de aceite. Use junto com o [`01-configuracoes-iniciais.md`](01-configuracoes-iniciais.md) — esta doc detalha o que cada tela faz; aquela define a ordem de cadastro.

> Convenção de rotas: tudo abaixo é prefixado por `/release-orchestrator/` (exceto Login e Dashboard global), porque o módulo está montado em [`app.routes.ts`](../../../nexus-portal-web/frontend/src/app/app.routes.ts) sob esse path.

---

## Índice

1. [Login](#1-login)
2. [Dashboard global](#2-dashboard-global)
3. [Dashboard do Release Orchestrator](#3-dashboard-do-release-orchestrator)
4. [Lista de produtos](#4-lista-de-produtos)
5. [Detalhe do produto — abas](#5-detalhe-do-produto)
6. [Lista de módulos do produto](#6-lista-de-módulos-do-produto)
7. [Lista de clientes](#7-lista-de-clientes)
8. [Form de cliente (novo / editar)](#8-form-de-cliente)
9. [Detalhe do cliente — abas](#9-detalhe-do-cliente)
10. [Lista de releases](#10-lista-de-releases)
11. [Form de release](#11-form-de-release)
12. [Detalhe da release — abas](#12-detalhe-da-release)
13. [Lista de próximas entregas (agenda)](#13-lista-de-próximas-entregas)
14. [Form de próxima entrega](#14-form-de-próxima-entrega)
15. [Lista de entregas (histórico)](#15-lista-de-entregas)
16. [Wizard de nova entrega (5 passos)](#16-wizard-de-nova-entrega)
17. [Tela de delta editável](#17-tela-de-delta-editável)
18. [Detalhe da entrega](#18-detalhe-da-entrega)
19. [Templates](#19-templates)
20. [Guia do release-orchestrator](#20-guia)

---

## 1. Login

### Caminho
`http://localhost:4200/login`

### Objetivo
Autenticar via JWT antes de qualquer rota do sistema (login é exigido pelo `AuthGuard` em todas as rotas do shell).

### Campos

| Campo | Obrigatório | Exemplo |
|---|---|---|
| E-mail / usuário | Sim | `admin@nexus.local` ou `admin` |
| Senha | Sim | `admin` |

### Comportamento esperado
- Submit → `POST /api/v1/auth/login` → retorna `{ token, refreshToken? }`.
- Token persiste em `localStorage` (`auth.token`).
- Próximas chamadas levam `Authorization: Bearer <token>` via interceptor.
- Redireciona para a home (`/` → cards dos módulos).

### Validação visual
- Sem credenciais válidas → toast vermelho "Usuário ou senha inválidos".
- Com sucesso → dashboard global aparece em < 2s.

### Banco
```sql
SELECT username, ativo FROM tb_usuario WHERE username = 'admin';
-- ativo = TRUE
```

### Critérios de aceite
- [ ] Erro 401 mostra mensagem clara.
- [ ] Após login, qualquer rota do release-orchestrator abre sem prompt de novo login.
- [ ] Refresh da página mantém sessão (token continua válido até expirar).

---

## 2. Dashboard global

### Caminho
`http://localhost:4200/`

### Objetivo
Home do portal — mostra cards por módulo (DocFlow, Release Orchestrator, Segurança) com base nas permissões do usuário.

### Comportamento esperado
- Faz `GET /api/v1/auth/me` para resolver grupos e permissões.
- Esconde cards de módulos que o usuário não tem permissão para ver.
- `admin` enxerga os 3 módulos; `editor` no estado seed atual enxerga só Segurança.

### Critérios de aceite
- [ ] Card "Release Orchestrator" aparece e abre `/release-orchestrator`.
- [ ] Menu lateral lista as opções do módulo.

---

## 3. Dashboard do Release Orchestrator

### Caminho
`/release-orchestrator`

### Objetivo
Visão executiva: KPIs, agenda de próximas entregas, entregas recentes.

### Conteúdo esperado

| Bloco | Origem |
|---|---|
| KPI "Entregas no mês" | `GET /entregas?dataInicio=...&dataFim=...` |
| KPI "Entregas pendentes" | `GET /proximas-entregas?status=PLANEJADA,AGENDADA` |
| KPI "Entregas em geração" | `GET /entregas?status=EM_GERACAO` |
| KPI "Falhas recentes" | `GET /entregas?status=FALHA` |
| Agenda (próximos 7 dias) | `GET /proximas-entregas?data_prevista=hoje+7d` |
| Entregas recentes (últimas 10) | `GET /entregas?size=10&sort=createdAt,desc` |

### Validação visual
- Loading spinner ao abrir.
- KPIs com cor distinta para entregas com `FALHA`.
- Clicar em "Próximas entregas" abre a lista filtrada.

### Critérios de aceite
- [ ] Sem dados → KPIs zerados, mensagem "Nenhuma entrega ainda".
- [ ] Com dados → contadores conferem com queries SQL equivalentes.
- [ ] Polling: dashboard atualiza automaticamente (ou recarrega ao receber notificação).

---

## 4. Lista de produtos

### Caminho
`/release-orchestrator/produtos`

### Objetivo
Listar e gerenciar produtos cadastrados.

### Colunas / cards visíveis

| Coluna | Origem |
|---|---|
| Sigla | `tb_produto_rh.sigla` |
| Nome | `tb_produto_rh.nome` |
| Cor | badge |
| Ativo | badge verde/cinza |
| Qtd módulos | count(`tb_modulo_produto`) |
| Última release publicada | max(`tb_release.versao` where status=PUBLICADA) |

### Ações
- Botão **Novo produto** → abre form modal/página.
- Card clicável → vai para detalhe.
- Filtro: ativo/inativo + busca por nome.

### Critérios de aceite
- [ ] Produto recém-criado aparece sem refresh manual.
- [ ] Filtro "Apenas ativos" esconde inativos.
- [ ] Click no card abre `/release-orchestrator/produtos/{id}` (ou abre aba Geral se for tab-based).

---

## 5. Detalhe do produto

### Caminho
`/release-orchestrator/produtos/{id}` (ou modal/abas dentro da lista, conforme implementação)

### Abas esperadas

| Aba | O que valida | Spec |
|---|---|---|
| **Geral** | Sigla, nome, cor, ativo | `01 §2` |
| **GitHub** | Repo + branch + regex + PAT + **Testar conexão** | `01 §3` |
| **Jenkins** | URL + job + user + token + trigger mode + **Testar conexão** | `01 §4` |
| **Módulos** | Lista de módulos com Novo/Editar/Excluir | `01 §5` |
| **Catálogo funcional** | Domínios + funcionalidades aninhadas | `01 §6` |

### Validação por aba — pontos críticos

#### Aba Geral
- Salvar com sigla repetida deve mostrar erro 409 ("Sigla já cadastrada").
- Inativar produto: módulos e contratos continuam, mas o produto não aparece em `Nova entrega → Passo 1`.

#### Aba GitHub
- Campo **Token** nunca devolve o valor salvo no GET subsequente (apenas placeholder `••••`).
- **Testar conexão** com PAT inválido → erro "401 Unauthorized" + texto do erro do GitHub.
- Com tag regex inválido → erro de validação client-side antes do POST.

#### Aba Jenkins
- Idem token.
- **Testar conexão** mostra último build OK/FAIL/EM_ANDAMENTO se houver.

#### Aba Módulos
- Edição inline ou modal.
- Excluir módulo com artefatos vinculados → confirmar 409 e mensagem amigável ("Módulo possui artefatos em releases publicadas").

#### Aba Catálogo funcional
- Hierarquia: domínio expandível mostra funcionalidades dentro.
- Drag-and-drop ou campo ordem para reordenar.

### Critérios de aceite (todas as abas)
- [ ] Cada **Salvar** mostra toast de sucesso e atualiza a tela sem F5.
- [ ] Banco confirma mudanças em `tb_produto_rh` / `tb_modulo_produto` / `tb_dominio_produto` / `tb_funcionalidade_produto`.
- [ ] `/actuator/health/github` e `/actuator/health/jenkins` viram `UP` após salvar as integrações.

---

## 6. Lista de módulos do produto

### Caminho
`/release-orchestrator/produtos/{id}/modulos`

### Comportamento
Mesma funcionalidade da aba Módulos — disponível como página própria para edição mais ampla, especialmente útil para popular `config_especifica` de BANCO/KETTLE (campo JSON grande).

### Validação
- JSON inválido em `config_especifica` → toast com erro "JSON malformado" e linha/coluna se possível.
- Salvar com `config_especifica = null` em módulo BANCO/KETTLE → permitido (delta não vai gerar nada útil, mas não bloqueia).

### Critérios de aceite
- [ ] Módulo BANCO mostra campo dedicado para dialetos.
- [ ] Validação JSON acontece **antes** do POST (client-side).

---

## 7. Lista de clientes

### Caminho
`/release-orchestrator/clientes`

### Colunas

| Coluna | Origem |
|---|---|
| Sigla | `tb_cliente_orchestrator.sigla` |
| Nome | `nome` |
| CNPJ | `cnpj` (formatado) |
| Ambiente padrão | `ambiente_padrao` |
| Ativo | badge |
| Qtd produtos contratados | count(`tb_cliente_produto`) |

### Ações
- **Novo cliente** → `/release-orchestrator/clientes/novo`.
- Card clicável → detalhe.
- Filtros: ativo, ambiente, busca por sigla/nome/CNPJ.

### Critérios de aceite
- [ ] Filtros persistem na URL (query params).
- [ ] Paginação ou virtual scroll com 50+ clientes.

---

## 8. Form de cliente

### Caminho
- Novo: `/release-orchestrator/clientes/novo`
- Editar: `/release-orchestrator/clientes/{id}/editar`

### Estrutura
Form único com seções:

| Seção | Campos |
|---|---|
| **Identificação** | Sigla, Nome, Razão social, CNPJ |
| **Operacional** | Ambiente padrão, Tipo banco, Codificação, Fuso horário |
| **Contatos** | Lista editável (Nome, Papel, E-mail, Telefone) |
| **Observações** | Texto livre |

### Validações client-side
- Sigla: somente A-Z, 0-9, hífen, ≤ 20 chars.
- CNPJ: 14 dígitos (sem máscara obrigatória, mas valida dígito verificador).
- E-mail: regex padrão.
- Ambiente: enum estrito.

### Comportamento
- `CanDeactivateGuard` ativo: se houver alterações não salvas, mostra confirm antes de sair.
- Botões: **Cancelar** | **Salvar** | **Salvar e adicionar produto** (atalho para o próximo cadastro).

### Persistência
- Novo: `POST /clientes` → cria `tb_cliente_orchestrator` + contatos em batch.
- Editar: `PUT /clientes/{id}`.

### Critérios de aceite
- [ ] Sair sem salvar com mudanças → modal de confirmação.
- [ ] Após salvar, vai para detalhe do cliente (aba Geral selecionada).
- [ ] Contatos inseridos viram linhas em `tb_contato_orchestrator`.

---

## 9. Detalhe do cliente

### Caminho
`/release-orchestrator/clientes/{id}`

### Abas

| Aba | O que mostra |
|---|---|
| **Visão geral** | Cards com KPIs do cliente: total de entregas, última entrega, próxima agendada |
| **Contatos** | Lista CRUD de contatos |
| **Produtos** | Contratos + módulos contratados com `versao_atual` editável |
| **Funcionalidades** | Matriz domínio × funcionalidade (só funcionalidades dos produtos contratados) |
| **Config. entrega** | Form do destino (PASTA/FTP/SFTP/BUCKET) + **Testar conexão** |
| **Próximas entregas** | Lista das entregas agendadas para este cliente |

### Pontos críticos por aba

#### Visão geral
- KPI "Última entrega" deve mostrar versão entregue (max `tb_entrega` por `cliente_id`).
- Botão **Nova entrega para este cliente** atalha o wizard com cliente prefilled.

#### Produtos
- Contratar produto → modal com dropdown de produtos ativos.
- Após adicionar, **expandir** mostra módulos do produto com inputs de `versao_atual` e checkbox `ativo`.
- Salvar versão atual → `PATCH /clientes/{id}/produtos/{cp_id}/modulos/{mod_id}` (ou bulk).

#### Funcionalidades
- Linhas vêm das funcionalidades dos produtos contratados.
- Toggle Habilitada → `PATCH /clientes/{id}/funcionalidades` (bulk).
- Origem `MANUAL` quando o operador alterna; `HERDADA` se vier de template.

#### Config. entrega
- Form muda dinamicamente conforme `tipo_destino`.
- **Testar conexão** sempre disponível depois de preencher campos mínimos.
- Campo senha em edição: placeholder "(preenchida)"; deixar em branco preserva.

#### Próximas entregas
- Lista filtrável (status, data).
- Botão **Converter em entrega** → abre wizard com cliente + produto + release prefilled.

### Critérios de aceite (consolidado)
- [ ] Cada aba carrega dados ao trocar (lazy load).
- [ ] Banco confirma mudanças em `tb_cliente_*`.
- [ ] **Testar conexão** verde antes de qualquer entrega remota.

---

## 10. Lista de releases

### Caminho
`/release-orchestrator/releases`

### Colunas

| Coluna | Origem |
|---|---|
| Produto | `tb_produto_rh.sigla` |
| Versão | `tb_release.versao` |
| Título | `titulo` |
| Tipo | `MAJOR`/`MINOR`/`PATCH` |
| Status | badge (RASCUNHO/EM_REVISAO/APROVADA/PUBLICADA) |
| Build | badge clicável (verde SUCCESS / vermelho FAILED / amarelo EM_ANDAMENTO) |
| Data publicação | `data_publicacao` |
| Itens | count(`tb_release_item`) |

### Ações
- **Nova release** → `/release-orchestrator/releases/nova`.
- Filtros: produto, status, intervalo de datas.
- Card de release clicável → detalhe.

### Critérios de aceite
- [ ] Badge de build aparece somente em releases com `ultimo_build_status` preenchido.
- [ ] Click no badge de build abre o build no Jenkins em nova aba.
- [ ] Filtro "Status = PUBLICADA" lista apenas releases prontas para virar entrega.

---

## 11. Form de release

### Caminho
- Novo: `/release-orchestrator/releases/nova`
- Editar: `/release-orchestrator/releases/{id}/editar`

### Campos

| Campo | Obrigatório | Observação |
|---|---|---|
| Produto | Sim | Dropdown com produtos ativos |
| Versão | Sim | UNIQUE por produto; semver recomendado |
| Tipo | Sim | `MAJOR`/`MINOR`/`PATCH` |
| Título | Sim | Mostrado em cabeçalho de PDF e UI |
| Resumo | Não | Markdown — vira parágrafo de abertura do PDF |
| Data prevista | Não | Para agenda |

### Comportamento
- Salvar → cria release em `RASCUNHO`.
- Após salvar, vai para detalhe com 3 abas (Itens, Artefatos, Histórico).
- `CanDeactivateGuard` ativo.

### Critérios de aceite
- [ ] Versão duplicada no mesmo produto → erro 409 com mensagem clara.
- [ ] Salvar sem título → validação client-side bloqueia submit.

---

## 12. Detalhe da release

### Caminho
`/release-orchestrator/releases/{id}`

### Header
- Título + versão + badges (status, build).
- Botões de transição de status: **Enviar para revisão**, **Aprovar**, **Publicar** — habilitam conforme status atual.
- Botão **Gerar PDF** com dropdown de tipo (`CLIENTE`/`SUPORTE`/`INTERNO`).

### Abas

#### Aba Itens
Lista de `tb_release_item` agrupada por categoria.

| Coluna | Notas |
|---|---|
| Categoria | NOVIDADE / MELHORIA / CORRECAO / BREAKING_CHANGE |
| Título | Editável inline ou modal |
| Visibilidade | TODOS / CLIENTE_X (filtra no PDF) |
| Ticket/Commit/PR | Links externos |
| Ordem | Drag-and-drop ou campo |

#### Aba Artefatos
- Lista de `tb_artefato_release_modulo` agrupada por módulo.
- Botão **Sincronizar do GitHub** no header (visível apenas se produto tem GitHub configurado).
- Para cada módulo: upload manual (drag-and-drop ou input file), lista de artefatos com SHA-256, tamanho, botão **Excluir** (bloqueado se release `PUBLICADA`).
- Coluna **Versão do módulo nesta release**: input de texto que persiste em `tb_release_modulo_versao`.

#### Aba Histórico
- Linha do tempo de `tb_release_historico`.
- Cada entrada: ação, status anterior → novo, usuário, timestamp.

### Validações
- Transição inválida (ex.: PUBLICADA → RASCUNHO) bloqueada e botão escondido.
- Editar item após PUBLICADA bloqueado.
- Excluir artefato após PUBLICADA bloqueado.

### Critérios de aceite
- [ ] Transição RASCUNHO → ... → PUBLICADA grava 3 linhas em `tb_release_historico`.
- [ ] PDF gerado para os 3 tipos (CLIENTE/SUPORTE/INTERNO) tem conteúdo diferenciado.
- [ ] **Sincronizar do GitHub** baixa assets e popula `tb_artefato_release_modulo` com SHA-256.

---

## 13. Lista de próximas entregas

### Caminho
`/release-orchestrator/proximas-entregas`

### Objetivo
Agenda de entregas planejadas (ainda não geradas).

### Colunas

| Coluna | Origem |
|---|---|
| Cliente | sigla |
| Produto | sigla |
| Release | versão (link p/ detalhe) |
| Data prevista | `data_prevista` |
| Ambiente | `PROD`/`HOM`/... |
| Prioridade | `BAIXA`/`MEDIA`/`ALTA`/`CRITICA` |
| Status | `PLANEJADA`/`AGENDADA`/`REPLANEJADA`/`ATRASADA`/`CONVERTIDA`/`CANCELADA` |
| Responsável | usuário |

### Ações
- **Nova entrega prevista** → form.
- **Converter em entrega** (atalho para wizard com prefill).
- **Cancelar** (muda status).

### Critérios de aceite
- [ ] Filtros persistem na URL.
- [ ] Status ATRASADA aparece automático se `data_prevista < hoje` e ainda não CONVERTIDA/CANCELADA.

---

## 14. Form de próxima entrega

### Caminho
- Novo: `/release-orchestrator/proximas-entregas/nova`
- Editar: `/release-orchestrator/proximas-entregas/{id}/editar`

### Campos

| Campo | Obrigatório | Exemplo |
|---|---|---|
| Cliente | Sim | `XPTO` |
| Produto | Sim | `Nexus` (apenas produtos contratados pelo cliente) |
| Release alvo | Não | `1.1.0` (se ainda não decidida, deixar vazio) |
| Data prevista | Sim | `2026-07-15` |
| Ambiente | Sim | `HOM` |
| Prioridade | Sim | `MEDIA` |
| Responsável | Não | usuário do sistema |
| Observações | Não | texto |

### Critérios de aceite
- [ ] Produto restringe pelos contratados do cliente selecionado.
- [ ] Release restringe por produto + status PUBLICADA.
- [ ] Salvar → linha em `tb_proxima_entrega` com status `PLANEJADA`.

---

## 15. Lista de entregas

### Caminho
`/release-orchestrator/entregas`

### Objetivo
Histórico de entregas geradas (e em geração).

### Colunas

| Coluna | Origem |
|---|---|
| Cliente | sigla |
| Produto + versão release | `Nexus 1.1.0` |
| Status | badge (RASCUNHO/EM_GERACAO/CONCLUIDA/FALHA/CANCELADA) |
| Status publicação | badge (NAO_APLICAVEL/PENDENTE/OK/FALHA) |
| Tamanho | bytes formatados |
| Data conclusão | `data_conclusao` |
| Responsável | usuário |

### Filtros
- Cliente, produto, status, status publicação, intervalo de datas.
- Filtros persistem na URL (compartilhável).

### Ações por linha
- **Ver detalhes** → `/entregas/{id}`.
- **Baixar pacote** (se `CONCLUIDA` e tipo PASTA — caminho local) → `GET /entregas/{id}/pacote`.
- **Reentregar** (se `CONCLUIDA`) → cria nova entrega com `entrega_original_id`.

### Critérios de aceite
- [ ] Lista atualiza em real-time entregas `EM_GERACAO` (polling 5s ou WebSocket).
- [ ] Filtro "Status = FALHA" lista exatamente as falhas (validar contra SQL).

---

## 16. Wizard de nova entrega

### Caminho
`/release-orchestrator/entregas/nova`

### Estrutura
Wizard de 5 passos com barra de progresso e validação por etapa.

#### Passo 1 — Cliente e produto

| Campo | Obrigatório | Notas |
|---|---|---|
| Cliente | Sim | Dropdown ativos |
| Produto | Sim | Restringe pelos contratados |
| Ambiente | Sim | Default = ambiente do contrato |

Botão **Próximo** habilita só com os 3 preenchidos.

#### Passo 2 — Release e responsável

| Campo | Obrigatório | Notas |
|---|---|---|
| Release | Sim | Apenas PUBLICADAS do produto |
| Responsável | Sim | Default = usuário logado |
| Observações | Não | Texto livre |

#### Passo 3 — Módulos a entregar

Tabela com todos os módulos do produto. Por linha:

| Coluna | Notas |
|---|---|
| Ativo | Toggle (default = ativo no contrato) |
| Módulo | Código + nome |
| Fora do contrato | Badge se o módulo não está em `tb_cliente_produto_modulo` |
| FROM | Versão atual do contrato (editável) |
| TO | Versão da release (read-only ou editável) |

Botão **Próximo** habilita se ≥ 1 módulo marcado.

#### Passo 4 — Delta

Botão grande **Calcular delta** → `POST /entregas/{rascunho_id}/delta/calcular`.

Após calcular, mostra tabela:

| Coluna | Notas |
|---|---|
| Módulo | código |
| Tipo | WEB/BATCH/BANCO/KETTLE |
| Contagem | "+N" arquivos no delta |
| Tamanho total | bytes |
| Detalhes | botão expand → lista de arquivos |

- WEB/BATCH: lista artefatos da release.
- BANCO: lista scripts SQL alterados entre FROM e TO no GitHub, agrupados em `DDL.sql` + `DML.sql` (por dialeto).
- KETTLE: lista `.ktr`/`.kjb` alterados + dependências resolvidas.

Mostra **total agregado** no header.

#### Passo 5 — Documento e geração

- Editor Markdown com release-notes pré-preenchido a partir dos itens + observações.
- Filtro automático por funcionalidades habilitadas do cliente (não mostra itens "TODOS" que mencionam funcionalidades não licenciadas).
- Botão **Gerar entrega** azul, canto direito.
- Ao clicar: cria `Entrega`, marca `EM_GERACAO`, dispara worker `@Async`, redireciona para `/entregas/{id}`.

### Validações entre passos
- Voltar mantém os dados (state guardado em service Angular).
- Sair com `CanDeactivateGuard` confirma descarte.
- Refresh do navegador no meio do wizard perde o estado (esperado; rascunho ainda não foi salvo).

### Critérios de aceite
- [ ] Cada passo bloqueia o próximo enquanto faltam obrigatórios.
- [ ] Passo 4 mostra contagem correta no badge "+N".
- [ ] Passo 5 mostra preview do PDF antes de gerar.
- [ ] Após **Gerar entrega**, redireciona em < 1s.

---

## 17. Tela de delta editável

### Caminho
`/release-orchestrator/entregas/{id}/delta`

### Objetivo
Permitir ajustar o range FROM..TO **depois** que a entrega já foi rascunhada — útil quando o operador percebe que precisa pular versões.

### Conteúdo

| Bloco | Notas |
|---|---|
| Header | Cliente, produto, release, status atual |
| Por módulo | Toggle ativo, input FROM editável, TO read-only, campo justificativa |
| Botão **Recalcular** | Re-roda `POST /entregas/{id}/delta/calcular` |
| Preview de arquivos | Mesma tabela do passo 4 do wizard |

### Comportamento
- Mudar FROM e clicar **Recalcular** atualiza preview sem persistir.
- Botão **Salvar override** persiste em `tb_entrega_modulo.versao_from` + `justificativa`.
- Se entrega já está `CONCLUIDA`, tela vira read-only (mostra delta que foi usado).

### Critérios de aceite
- [ ] FROM editado é refletido nos arquivos calculados.
- [ ] Justificativa obrigatória ao salvar override (deixar em branco → toast vermelho).
- [ ] Read-only se status ≠ `RASCUNHO`/`EM_GERACAO`.

---

## 18. Detalhe da entrega

### Caminho
`/release-orchestrator/entregas/{id}`

### Header
- Sigla cliente + produto + versão release + tipo (Cliente/Suporte/Interno).
- Badge **Status entrega** (RASCUNHO/EM_GERACAO/CONCLUIDA/FALHA/CANCELADA).
- Badge **Build** (se release tem `ultimo_build_status`).
- Badge **Publicação remota** (se destino ≠ PASTA): NAO_APLICAVEL/PENDENTE/OK/FALHA.
- Ações: **Baixar pacote** (CONCLUIDA), **Reentregar** (CONCLUIDA), **Cancelar** (EM_GERACAO).

### Polling
- Página faz polling de 5s enquanto status = `EM_GERACAO`.
- Para de polling ao virar `CONCLUIDA`/`FALHA`/`CANCELADA`.

### KPIs (topo)

| KPI | Origem |
|---|---|
| Módulos selecionados | count(`tb_entrega_modulo` where selecionado) |
| Itens no delta | count(`tb_entrega_modulo_artefato`) |
| Tamanho do pacote | `tamanho_bytes` |
| SHA-256 do pacote | `arquivo_pacote_sha256` (clipboard) |

### Cards visíveis quando CONCLUIDA

#### Card "Pacote"
- Nome do ZIP, SHA-256, caminho local, botão **Baixar pacote**.

#### Card "Publicação remota" (só se destino ≠ PASTA)
- Badge + próxima tentativa + destino final + motivo da última falha.
- Botão **Tentar agora** (força reagendamento).

### Abas

| Aba | Conteúdo |
|---|---|
| **Módulos** | Lista cada módulo: tipo, FROM, TO, total de itens |
| **Delta** | Lista detalhada dos itens — útil para suporte rastrear "o que foi entregue" |
| **Documento** | PDF release-notes embutido (iframe) com botão de download |
| **Histórico** | Eventos: criada, geração iniciada, concluída, publicação tentativa N, etc. |

### Validações
- Falha mostra mensagem em vermelho com `falha_motivo`.
- Cancelar `EM_GERACAO` interrompe worker (best-effort) e marca `CANCELADA`.

### Critérios de aceite
- [ ] Polling para automaticamente ao concluir.
- [ ] Badge de publicação vira `OK` em ≤ 2 min após geração (com destino remoto).
- [ ] **Tentar agora** dispara `POST /entregas/{id}/publicacao/reagendar` e badge volta para `PENDENTE`.
- [ ] **Baixar pacote** retorna o ZIP correto (SHA-256 bate com o exibido).

---

## 19. Templates

### Caminho
`/release-orchestrator/templates`

### Objetivo
Gerenciar templates de release (estruturas pré-definidas de itens por tipo).

### Comportamento esperado
- Lista templates existentes.
- CRUD básico.
- No form de nova release, dropdown "Aplicar template" pré-preenche itens.

### Critérios de aceite
- [ ] Template aplicado popula itens em RASCUNHO, sem persistir até salvar release.
- [ ] `tb_release_template` reflete o cadastro.

> **Nota:** se essa funcionalidade ainda não estiver implementada na UI (verificar `RfTemplatesComponent`), marcar como "tela mínima — placeholder" e seguir.

---

## 20. Guia

### Caminho
`/release-orchestrator/guia`

### Objetivo
Documentação inline do módulo (não consome backend).

### Critérios de aceite
- [ ] Links internos funcionam.
- [ ] Conteúdo descreve fluxo end-to-end (mesmo dos guias em `docs/jornadas/`).

---

## Mensagens de erro padrão (espera-se em todas as telas)

| Cenário | Mensagem esperada | HTTP |
|---|---|---|
| Token expirado | "Sessão expirada. Faça login novamente." | 401 |
| Sem permissão | "Você não tem permissão para esta ação." | 403 |
| Recurso não encontrado | "Registro não encontrado." | 404 |
| Conflito (UNIQUE) | Mensagem específica (ex.: "Sigla já cadastrada") | 409 |
| Validação | Mensagem por campo (ex.: "Sigla obrigatória") | 400 |
| Erro inesperado | "Erro inesperado. Tente novamente." | 500 |

Toda mensagem 5xx deve ter `correlationId` exibido para o usuário (canto inferior do toast), referenciando o log do backend.

---

## Validação cross-cutting

Independente da tela, sempre verifique:

- [ ] Toast de sucesso aparece após cada **Salvar**.
- [ ] Loading state visível em ações > 200 ms.
- [ ] `CanDeactivateGuard` ativa em todos os forms longos (cliente, release, módulo).
- [ ] Botões com ação destrutiva (excluir, cancelar, publicar) pedem confirmação.
- [ ] Filtros persistem na URL onde aplicável.
- [ ] `correlationId` aparece no header de toda resposta (DevTools → Network → Response Headers).

Próximo passo: [`03-fluxo-completo-entrega.md`](03-fluxo-completo-entrega.md) — executa o fluxo XPTO/Nexus ponta a ponta.
