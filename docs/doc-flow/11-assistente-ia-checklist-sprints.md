# 11 — Assistente IA — Checklist por sprint

Plano executável a partir do desenho [`10-assistente-ia-paginas.md`](10-assistente-ia-paginas.md) e da UX [`nexus-portal-web/docs/docflow/07-assistente-ia-paginas.md`](../../../nexus-portal-web/docs/docflow/07-assistente-ia-paginas.md).

**Legenda:** `[ ]` pendente · `[x]` concluído · **API** backend · **WEB** frontend · **DB** migration · **OPS** config/infra  
**Duração sugerida:** ~1–2 semanas por sprint (ajustar ao time).

---

## Visão rápida

| Sprint | Fase | Foco | Saída verificável | Status |
|---|---|---|---|---|
| **S1** | A | Fundação API | Sessão + perguntas (triagem heurística) | ✅ scaffold |
| **S2** | A | Geração async | Job → proposta HTML sanitizada + SSE | ✅ |
| **S3** | A | UI + aplicar | Wizard/drawer → form preenchido | ✅ |
| **S4** | A | Hardening | Auditoria, e2e, flag off, polish prompts | ✅ |
| **S5** | B | Atualizar página | Chat no editor + diff + PUT | 📋 |
| **S6** | C | PR → fila | Webhook merge → propostas-ia | 📋 |
| **S7+** | D | Aprendizado | Métricas, rejeições, permissões AI_* | 📋 |

**Dependência:** S1→S2→S3→S4 (MVP usável). S5 e S6 podem paralelizar depois de S4. S7 só com uso real.

---

## Sprint 1 — Fundação API (triagem)

**Objetivo:** abrir sessão, persistir mensagens e devolver perguntas (ou “pronto para gerar”) sem HTML ainda.

### Tickets

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| AI-101 | DB | Migration `tb_ai_sessao` + `tb_ai_mensagem` (+ enums/constraints) | ✅ `V16__ai__01_tables.sql` |
| AI-102 | API | Entities + repositories `AiSessao`, `AiMensagem` | ✅ |
| AI-103 | API | `AiProperties` + yml/env `NEXUS_AI_*` (módulo `nexus-ai`) | ✅ scaffold; dev `enabled=true` |
| AI-104 | API | Interface `LlmProvider` + `FakeLlmProvider` (dev/test) | ✅ scaffold + teste |
| AI-105 | API | `OpenAiCompatibleProvider` (HTTP) | ✅ scaffold; usado quando enabled+key |
| AI-106 | API | `AiSessaoService` + `POST/GET /ai/sessoes` + `POST .../mensagens` | ✅ Auth `PAGINA:*`; 503 se AI off |
| AI-107 | API | Triagem: briefing → perguntas estruturadas (máx. 5) | ✅ heurística → `PRONTA_PARA_GERAR` |
| AI-108 | API | `POST .../cancelar` | ✅ |
| AI-109 | API | Security path `/api/v1/ai/**` autenticado | ✅ (JWT + PreAuthorize) |
| AI-110 | API | Testes unitários sessão + triagem | ✅ |

### Checklist fino

#### DB
- [x] AI-101 Migration sessao/mensagem

#### API
- [x] AI-102 Entities/repos
- [x] AI-103 Properties + env
- [x] AI-104 Fake provider
- [x] AI-105 Provider OpenAI-compatible
- [x] AI-106 Endpoints sessão/mensagens
- [x] AI-107 Orquestrador triagem
- [x] AI-108 Cancelar
- [x] AI-109 Security + logs
- [x] AI-110 Testes

**Demo S1:** Postman/curl — criar sessão com briefing curto → receber perguntas; briefing rico → `PRONTA_PARA_GERAR` (ou equivalente).

---

## Sprint 2 — Geração async + proposta

**Objetivo:** enfileirar job, gerar HTML a partir de template, sanitizar, pré-avaliar qualidade, notificar via SSE.

### Tickets

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| AI-201 | DB | Migration `tb_ai_job` + `tb_ai_proposta` | ✅ `V17__ai__02_jobs_propostas.sql` |
| AI-202 | API | Entities/repos job + proposta | ✅ |
| AI-203 | API | `POST .../gerar` → 202 + job `PENDENTE` | ✅ |
| AI-204 | API | `AiJobWorkerService` `@Async` | ✅ afterCommit |
| AI-205 | API | Escolher/aplicar template (`PaginaTemplateService.aplicar`) | ✅ via `DocFlowAiBridge` |
| AI-206 | API | Prompt geração + parse JSON | ✅ + FakeLlmProvider |
| AI-207 | API | Sanitização Jsoup | ✅ `AiHtmlSanitizer` |
| AI-208 | API | Pré-rodar `PaginaQualidadeService` | ✅ |
| AI-209 | API | `GET .../proposta` + SSE `GET /ai/eventos` | ✅ |
| AI-210 | API | `POST .../aplicar` FORM / PERSISTIR | ✅ |
| AI-211 | API | Testes worker com fake LLM | ✅ |

### Checklist fino

- [x] AI-201 DB job/proposta
- [x] AI-202 Entities
- [x] AI-203 Gerar 202
- [x] AI-204 Worker async
- [x] AI-205 Template âncora
- [x] AI-206 Prompt + JSON
- [x] AI-207 Sanitize
- [x] AI-208 Qualidade
- [x] AI-209 GET proposta + SSE
- [x] AI-210 Aplicar
- [x] AI-211 Testes

**Demo S2:** sessão → gerar → SSE SUCESSO → GET proposta com HTML de classes `df-doc-content`.

---

## Sprint 3 — UI wizard + aplicar no editor

**Objetivo:** autor usa a UI para colar briefing e cair no `pagina-form` preenchido.

### Tickets

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| AI-301 | WEB | Models + `AiAssistenteService` (HTTP + SSE/polling) | ✅ |
| AI-302 | WEB | `app-ai-perguntas` | ✅ |
| AI-303 | WEB | `app-ai-proposta-preview` (HTML + checklist) | ✅ |
| AI-304 | WEB | Wizard “Criar com IA” (brief → chat → revisar) | ✅ `/doc-flow/assistente` |
| AI-305 | WEB | CTA lista páginas + `?origem=ia` | ✅ |
| AI-306 | WEB | Integrar aplicar → `patchValue` no `pagina-form` | ✅ |
| AI-307 | WEB | Feature flag UI (`AiFeatureService`) | ✅ |
| AI-308 | WEB | Layout CSS preview \| checklist | ✅ |
| AI-309 | WEB | Testes unit component/service | ✅ |
| AI-310 | WEB | Regenerar OpenAPI client se aplicável | ⏭ hand-written client |

### Checklist fino

- [x] AI-301 Service/models (+ SSE `eventosAi`)
- [x] AI-302 `app-ai-perguntas`
- [x] AI-303 `app-ai-proposta-preview` (+ checklist)
- [x] AI-304 Wizard `/doc-flow/assistente` (passos Brief → Chat → Revisar)
- [x] AI-305 CTA lista “Criar com IA” + `?origem=ia`
- [x] AI-306 Aplicar no `pagina-form` (state + dirty)
- [x] AI-307 `AiFeatureService` (oculta CTAs se AI off)
- [x] AI-308 Layout preview | checklist (responsive)
- [x] AI-309 Testes unit (service/feature/componentes/CTA)
- [ ] AI-310 OpenAPI — cliente AI continua hand-written (`api:check` N/A)

**Demo S3:** UI — colar briefing → (responder perguntas) → Aplicar no editor → ver campos + HTML; salvar rascunho manualmente.

---

## Sprint 4 — Hardening MVP (Fase A fechada)

**Objetivo:** pronto para uso interno controlado.

### Tickets

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| AI-401 | API | Eventos auditoria AI_* | ✅ |
| AI-402 | API | Métricas mínimas (tokens, latência, status job) | ✅ log `ai.job.completed` + colunas job |
| AI-403 | API | Polish prompts (`AiPromptBuilder`) | ✅ |
| AI-404 | API | Limites + rate limit por usuário | ✅ 429 |
| AI-405 | WEB+API | Erros amigáveis | ✅ `mensagemErroHttp` + mensagens job |
| AI-406 | WEB | Playwright e2e com intercept | ✅ `ai-assistente-flow.spec.ts` |
| AI-407 | OPS | Runbook local `NEXUS_AI_*` | ✅ `docs/ai/RUNBOOK-LOCAL.md` |
| AI-408 | DOC | Contexto agentes + READMEs | ✅ |

### Checklist fino

- [x] AI-401 Auditoria
- [x] AI-402 Métricas
- [x] AI-403 Prompts
- [x] AI-404 Limites
- [x] AI-405 UX erros
- [x] AI-406 e2e
- [x] AI-407 Runbook
- [x] AI-408 Docs contexto

**Definition of Done Fase A:** critérios da §11 em `10-assistente-ia-paginas.md` todos marcados.

---

## Sprint 5 — Fase B (atualizar página)

**Objetivo:** na página aberta, pedir ajuste e aplicar com diff.

### Tickets

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| AI-501 | API | `objetivo=ATUALIZAR_PAGINA` + validar `paginaId` | 400 se faltar |
| AI-502 | API | Prompt com HTML atual (chunk por seções se grande) | Proposta `ATUALIZACAO` |
| AI-503 | API | Aplicar → `PaginaService.atualizar` (revisão) ou autosave | Status/version corretos |
| AI-504 | WEB | Toggle Assistente na toolbar do editor | Só `PAGINA:EDITAR` |
| AI-505 | WEB | Diff proposta vs atual (reusar comparador revisões) | Aceitar/rejeitar explícito |
| AI-506 | WEB | (Opcional) inserir/substituir seção no rich editor | Não quebra modo código |
| AI-507 | API+WEB | Testes update + diff | CI verde |

### Checklist fino

- [ ] AI-501 Objetivo update
- [ ] AI-502 Prompt contextual
- [ ] AI-503 Aplicar update
- [ ] AI-504 Toggle editor
- [ ] AI-505 Diff UI
- [ ] AI-506 Insert parcial (opc.)
- [ ] AI-507 Testes

**Demo S5:** editar página → “reescreva pré-requisitos” → diff → aplicar → histórico com revisão.

---

## Sprint 6 — Fase C (PR → fila)

**Objetivo:** merge em `main` gera proposta pendente para humano.

### Tickets

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| AI-601 | API | Webhook `POST /docflow/webhooks/github` + secret | 403 sem secret; 204 ok |
| AI-602 | API | Security `permitAll` só nesse path | Restante AI continua autenticado |
| AI-603 | DB | Idempotência por `delivery-id` (+ origem PR na proposta/sessão) | Retrys não duplicam |
| AI-604 | API | Cliente GitHub DocFlow (PR + files) | Não depende do módulo RO |
| AI-605 | API | Classificador UI_NOVA / UI_ALTERACAO / IRRELEVANTE | Backend-only ignorado |
| AI-606 | API | Match `codigoTela` → página existente | NOVA vs ATUALIZACAO |
| AI-607 | API | Abrir sessão + job a partir do PR | Reusa pipeline S2 |
| AI-608 | API | Listar / rejeitar / aceitar propostas (fila) | Paginação simples |
| AI-609 | WEB | Rota `/doc-flow/propostas-ia` + shell item | Lista acionável |
| AI-610 | WEB | Aceitar abre editor / cria RASCUNHO | Fluxo editorial normal depois |
| AI-611 | OPS | Runbook: configurar webhook GitHub + envs | Piloto em 1 repo |
| AI-612 | API+WEB | Testes webhook + classificador + e2e fila | CI verde |

### Checklist fino

- [ ] AI-601 Webhook
- [ ] AI-602 Security
- [ ] AI-603 Idempotência
- [ ] AI-604 GitHub client
- [ ] AI-605 Classificador
- [ ] AI-606 Match pagina
- [ ] AI-607 Pipeline sessão
- [ ] AI-608 API fila
- [ ] AI-609 UI fila
- [ ] AI-610 Aceitar
- [ ] AI-611 Runbook
- [ ] AI-612 Testes

**Demo S6:** PR de UI merged (ou payload fixture) → item na fila → aceitar → rascunho no editor.

---

## Sprint 7+ — Fase D (aprendizado)

### Tickets

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| AI-701 | API | Motivo de rejeição + campos alterados pós-aceite | Persistido e consultável |
| AI-702 | API/WEB | Dashboard leve (aceite %, latência p50, tokens) | Visível para ADMIN |
| AI-703 | API | Ajuste prompts com base nos padrões de rejeição | Ciclo documentado |
| AI-704 | DB+API+WEB | Permissões `PAGINA:AI_GERAR`, `AI_APLICAR`, `AI_PROPOSTA` | Seed RBAC + guards |

- [ ] AI-701 Feedback loop
- [ ] AI-702 Métricas UI
- [ ] AI-703 Prompt ops
- [ ] AI-704 Permissões granulares

---

## Ordem de ataque recomendada (primeiro PR de código)

1. **AI-101 → AI-103 → AI-104 → AI-102 → AI-106** (esqueleto compilando)
2. **AI-107 → AI-110** (triagem testável)
3. **AI-201 → AI-204 → AI-206 → AI-207 → AI-209** (primeira proposta de ponta a ponta API)
4. **AI-301 → AI-304 → AI-306** (primeira demo UI)
5. Fechar S4 antes de abrir S5/S6

---

## Fora de escopo (não criar ticket)

- Publicação automática pela IA
- Dependência Maven nova `ai` no MVP
- Reusar `GitHubReleasesAdapter` do Release Orchestrator para PRs (criar cliente DocFlow)
- Substituir o editor WYSIWYG
- Geração de imagens/capturas reais (manter `screen-placeholder`)

---

## Rastreio

| Doc | Papel |
|---|---|
| [`10-assistente-ia-paginas.md`](10-assistente-ia-paginas.md) | Desenho técnico |
| [`07-assistente-ia-paginas.md`](../../../nexus-portal-web/docs/docflow/07-assistente-ia-paginas.md) | UX front |
| Este arquivo | Backlog executável por sprint |
