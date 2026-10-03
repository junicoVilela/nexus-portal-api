# 10 — Assistente de IA para Páginas (DocFlow)

> Status: **Fase A implementada** (criação de página nova + importação de documento). Fases B–D pendentes — ver [`11`](11-assistente-ia-checklist-sprints.md).  
> Frontend espelho: `nexus-portal-web/docs/docflow/07-assistente-ia-paginas.md`  
> Contexto: DocFlow já tem templates, editor WYSIWYG, qualidade (`PaginaQualidadeService`), workflow `RASCUNHO → … → PUBLICADO`, SSE e integração runtime com provedor LLM compatível com OpenAI.

---

## 1. Objetivo

Acelerar a criação e manutenção de manuais de usuário com IA, **sempre com humano no loop**:

| Capacidade | Descrição | Fase |
|---|---|---|
| **Assistente conversacional** | Colar brief → perguntas opcionais → rascunho HTML no editor | **A (MVP)** |
| **Ajuste de página existente** | Chat sobre a página aberta (“reescreva a seção X…”) | **B** |
| **PR → proposta de página** | Merge em `main` gera proposta `NOVA`/`ATUALIZACAO` na fila editorial | **C** |
| **Aprendizado operacional** | Aceite/rejeite alimenta métricas e prompts | **D** |

Princípio duro: a IA **nunca** publica. Saída máxima = `RASCUNHO` ou proposta pendente de aceite.

---

## 2. Por que começar pela Fase A

1. Reaproveita 100% do caminho feliz atual: `PaginaTemplateService.aplicar` → form → `PaginaService.criar` / `autosave`.
2. Não depende de webhook GitHub nem de API de Pull Requests (hoje o adapter só cobre releases/compare/assets).
3. Valida prompts, sanitização HTML e UX de “proposta → aplicar no editor” antes de automatizar ingestão.

A Fase C reutiliza a **mesma fábrica de rascunhos** da Fase A.

---

## 3. Arquitetura alvo

```text
┌─────────────────────┐     ┌─────────────────────┐
│ Chat / paste (UI)   │     │ GitHub webhook (C)  │
└─────────┬───────────┘     └──────────┬──────────┘
          │                            │
          ▼                            ▼
┌──────────────────────────────────────────────────┐
│  DocFlow AI (com.nexus.portal.docflow.ai)        │
│  ┌────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │ Sessão     │  │ Orquestrador │  │ Provider  │ │
│  │ + mensagens│→ │ (prompts +   │→ │ LLM       │ │
│  └────────────┘  │ JSON schema) │  └───────────┘ │
│                  └──────┬───────┘                │
│                         ▼                        │
│            ┌────────────────────────┐            │
│            │ JobAiGeracao (@Async)  │            │
│            │ + SSE eventos          │            │
│            └────────────┬───────────┘            │
└─────────────────────────┼────────────────────────┘
                          ▼
               PropostaAi / campos do form
                          ▼
        PaginaService.criar | autosave | atualizar
                          ▼
        PaginaQualidadeService + workflow editorial
```

### Fronteira de módulo

| Opção | Decisão |
|---|---|
| Package dentro de `docflow` | Descartado — dificulta extração |
| **Maven `nexus-ai` + UI no DocFlow web** | **Escolhido** — API `/api/v1/ai/**`, UI `/doc-flow/assistente` |
| Acoplamento DocFlow | Só em `com.nexus.portal.ai.integration.docflow` (troca por HTTP na extração) |
| GitHub client | Fase C: `ai.integration.github` (não no Release Orchestrator) |

Pacotes:

```text
com.nexus.portal.ai
  ├── config/              # AiProperties
  ├── controller/          # AiStatusController, (S1+) sessões…
  ├── dto/
  ├── entity/              # AiSessao, AiMensagem, AiJob, AiProposta (S1+)
  ├── repository/
  ├── service/
  ├── provider/            # LlmProvider, Fake, OpenAI-compatible
  ├── prompt/
  └── integration/docflow/ # Única ponte com Pagina/Template/Qualidade
```

Scaffold já criado: ver [`../ai/README.md`](../ai/README.md).

---

## 4. Modelo de dados (Fase A)

### 4.1 `tb_ai_sessao`

| Coluna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `objetivo` | VARCHAR | `CRIAR_PAGINA` \| `ATUALIZAR_PAGINA` |
| `status` | VARCHAR | `ABERTA` \| `AGUARDANDO_USUARIO` \| `PRONTA_PARA_GERAR` \| `GERANDO` \| `PRONTA` \| `APLICADA` \| `CANCELADA` \| `ERRO` |
| `projeto_id` | UUID nullable | contexto |
| `modulo_id` | UUID nullable | |
| `cliente_id` | UUID nullable | para variáveis de template |
| `pagina_id` | UUID nullable | preenchido após aplicar / ou página alvo (Fase B) |
| `template_id` | UUID nullable | sugerido ou escolhido |
| `briefing` | TEXT | conteúdo colado inicial |
| `created_by` | VARCHAR(120) | |
| `created_at` / `updated_at` | timestamptz | |

### 4.2 `tb_ai_mensagem`

| Coluna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `sessao_id` | UUID FK | |
| `papel` | VARCHAR | `USUARIO` \| `ASSISTENTE` \| `SISTEMA` |
| `conteudo` | TEXT | texto livre da conversa |
| `payload_json` | JSONB nullable | perguntas estruturadas / metadados |
| `ordem` | INT | |
| `created_at` | timestamptz | |

### 4.3 `tb_ai_job`

Espelha o padrão de `Publicacao` (`GERANDO` → `SUCESSO`/`ERRO`).

| Coluna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `sessao_id` | UUID FK | |
| `tipo` | VARCHAR | `TRIAGEM` \| `GERAR_RASCUNHO` \| `AJUSTAR` |
| `status` | VARCHAR | `PENDENTE` \| `PROCESSANDO` \| `SUCESSO` \| `ERRO` |
| `erro_mensagem` | TEXT nullable | |
| `tokens_entrada` / `tokens_saida` | INT nullable | custo/observabilidade |
| `modelo` | VARCHAR nullable | |
| `started_at` / `finished_at` | timestamptz | |

### 4.4 `tb_ai_proposta`

Resultado estruturado pronto para o editor.

| Coluna | Tipo | Notas |
|---|---|---|
| `id` | UUID PK | |
| `sessao_id` | UUID FK | |
| `job_id` | UUID FK | |
| `tipo` | VARCHAR | `NOVA` \| `ATUALIZACAO` |
| `titulo` | VARCHAR(200) | |
| `slug` | VARCHAR(200) | |
| `codigo_tela` | VARCHAR(120) | |
| `resumo` | TEXT | |
| `conteudo_html` | TEXT | HTML sanitizado, classes `df-doc-content` |
| `template_id` | UUID nullable | |
| `template_versao` | INT nullable | |
| `qualidade_json` | JSONB | snapshot do checklist (pré-avaliação) |
| `status` | VARCHAR | `PENDENTE` \| `ACEITA` \| `REJEITADA` \| `DESCARTADA` |
| `pagina_id` | UUID nullable | preenchido ao aceitar |

Migration sugerida (nome ilustrativo): `Vxx__docflow__ai_assistente.sql` + seed de permissões no catálogo RBAC.

---

## 5. Contratos de API (Fase A)

Base: `/api/v1/ai`  
Proxy front: `/api/ai` → `/api/v1/ai`.

### 5.1 Sessão

| Método | Path | Auth | Papel |
|---|---|---|---|
| `POST` | `/ai/sessoes` | `PAGINA:CRIAR` | Abre sessão com briefing + contexto |
| `GET` | `/ai/sessoes/{id}` | `PAGINA:LER` | Estado + última proposta |
| `POST` | `/ai/sessoes/{id}/mensagens` | `PAGINA:CRIAR` ou `EDITAR` | Resposta do usuário / follow-up |
| `POST` | `/ai/sessoes/{id}/gerar` | `PAGINA:CRIAR` | Enfileira job `GERAR_RASCUNHO` → **202**. Corpo opcional `{ "instrucao": "…" }`: ajuste do autor enviado ao modelo junto com a PageSpec anterior |
| `GET` | `/ai/sessoes/{id}/proposta` | `PAGINA:LER` | Proposta atual |
| `POST` | `/ai/sessoes/{id}/proposta/rejeitar` | `PAGINA:CRIAR` | Proposta `PENDENTE` → `REJEITADA` com `motivo` opcional; sessão segue disponível para regenerar |
| `POST` | `/ai/sessoes/{id}/aplicar` | `PAGINA:CRIAR` | Materializa página `RASCUNHO` **ou** devolve payload para o form |
| `POST` | `/ai/sessoes/{id}/cancelar` | `PAGINA:CRIAR` ou `EDITAR` | Cancela sessão/job |

### 5.2 Request — criar sessão

```json
{
  "objetivo": "CRIAR_PAGINA",
  "briefing": "Cole aqui o texto / changelog / especificação...",
  "projetoId": "uuid",
  "moduloId": "uuid",
  "clienteId": "uuid-opcional",
  "templateId": "uuid-opcional",
  "paginaId": null
}
```

### 5.3 Response — mensagem do assistente (perguntas)

Quando faltar contexto, o orquestrador **não** gera HTML ainda:

```json
{
  "sessaoId": "…",
  "status": "AGUARDANDO_USUARIO",
  "mensagem": {
    "papel": "ASSISTENTE",
    "conteudo": "Para montar o guia, preciso de alguns detalhes:",
    "perguntas": [
      {
        "id": "publico",
        "texto": "Quem é o público desta tela?",
        "opcoes": ["Operador", "Gestor", "Ambos"],
        "obrigatoria": true
      },
      {
        "id": "codigoTela",
        "texto": "Qual o código da tela (codigoTela)?",
        "obrigatoria": true
      }
    ]
  }
}
```

Máximo recomendado: **5 perguntas**. Se o briefing já trouxer título + código + fluxo, pular direto para `gerar`.

Cada mensagem do assistente traz `contexto` (o que a triagem extraiu: `titulo`, `codigoTela`, `publico`…), exibido na UI para o autor corrigir. Se o `codigoTela` já pertence a outra página (`uq_tb_pagina_codigo_tela`), a triagem devolve pergunta obrigatória pedindo outro código.

### 5.4 Response — proposta

```json
{
  "id": "…",
  "tipo": "NOVA",
  "titulo": "Consulta de pedidos",
  "slug": "consulta-de-pedidos",
  "codigoTela": "PED-CONSULTA",
  "resumo": "Como filtrar e exportar pedidos no portal.",
  "conteudoHtml": "<section class=\"doc-intro\">…</section>",
  "templateId": "…",
  "templateVersao": 3,
  "qualidade": {
    "aptoParaRevisao": false,
    "itens": [
      { "codigo": "CAPTURA", "ok": false, "severidade": "AVISO", "titulo": "Captura pendente" }
    ]
  },
  "status": "PENDENTE"
}
```

### 5.5 Aplicar proposta

Dois modos (configuráveis; default = `FORM`):

| Modo | Comportamento |
|---|---|
| `FORM` | Retorna DTO compatível com `PaginaRequest` / form; UI preenche `pagina-form` sem persistir ainda |
| `PERSISTIR` | Chama `PaginaService.criar` com status `RASCUNHO`, marca proposta `ACEITA`, sessão `APLICADA` |

Recomendação MVP: **`FORM`** no fluxo “novo”; **`PERSISTIR`** só quando já houver `editId` e o usuário confirmar overwrite parcial (Fase B).

### 5.6 SSE de jobs

`GET /api/v1/ai/eventos` — mesmo padrão de `PublicacaoEventService`:

```json
{ "jobId": "…", "sessaoId": "…", "status": "PROCESSANDO|SUCESSO|ERRO", "progresso": 40 }
```

Frontend: subscribe + fallback polling `GET /ai/sessoes/{id}` a cada 3–5s (como `entrega-detalhe`).

---

## 6. Orquestração LLM

### 6.1 Provider

```text
nexus.ai.enabled=true
nexus.ai.base-url=${NEXUS_AI_BASE_URL:https://openrouter.ai/api/v1}
nexus.ai.api-key=${NEXUS_AI_API_KEY:}
nexus.ai.model=${NEXUS_AI_MODEL:openai/gpt-5.6-luna}
nexus.ai.reasoning-effort=${NEXUS_AI_REASONING_EFFORT:low}
nexus.ai.http-referer=${NEXUS_AI_HTTP_REFERER:http://localhost:4200}
nexus.ai.app-title=${NEXUS_AI_APP_TITLE:Nexus AI}
nexus.ai.timeout-seconds=90
nexus.ai.max-perguntas=5
nexus.ai.max-tokens-saida=8000
```

Classe: `AiProperties` (`@ConfigurationProperties(prefix = "nexus.ai")`) no módulo `nexus-ai`.  
Provider padrão: **OpenRouter** (headers `HTTP-Referer` + `X-Title`).

### 6.2 Pipeline do job `GERAR_RASCUNHO`

1. Carregar sessão + mensagens + contexto (projeto, módulo, template HTML se houver).
2. Se `templateId` ausente: sugerir template por similaridade de briefing e confiança explicável.
3. Resolver o blueprint editorial associado ao template; manter componentes obrigatórios e
   recomendados e adicionar opcionais somente quando o briefing justificar.
4. Montar o prompt com briefing, Q&A, blueprint e somente os componentes candidatos.
5. Exigir **JSON Schema** estrito para a `PageSpec`, sem HTML livre produzido pelo modelo.
6. Registrar `schemaVersion`/`blueprintId`, validar IDs/slots, renderizar no servidor e sanitizar
   o fragmento final com Jsoup.
7. Pré-rodar regras de `PaginaQualidadeService` sobre um `Pagina` transitório.
8. Persistir `AiProposta` + job `SUCESSO` + SSE.

### 6.3 Regras de conteúdo (prompts)

O modelo não escreve HTML: devolve uma `PageSpec` (componentes do catálogo + textos por slot),
renderizada e sanitizada no servidor. O antigo caminho de "preencher o esqueleto HTML" foi
removido. As regras de redação vivem em arquivos versionados:
[`ai/src/main/resources/prompts/`](../../ai/src/main/resources/prompts/README.md). Cada
proposta registra `prompt_versao`. Briefing e manifesto de documento vão entre marcas
`<<<…`/`…>>>` e o system prompt os trata como dado, não como instrução.

### 6.4 Fallback sem provider

Se `docflow.ai.enabled=false` ou chave ausente: endpoints retornam **503** com mensagem clara; UI esconde o painel (feature flag).

---

## 7. UX Frontend (Fase A)

Spec detalhada: [`07-assistente-ia-paginas.md`](../../../nexus-portal-web/docs/docflow/07-assistente-ia-paginas.md).

Resumo:

1. Entrada em `/doc-flow/paginas/novo?origem=ia` **ou** botão “Criar com IA” no `pagina-form` / lista.
2. Painel lateral (drawer) ou passo 0 do wizard de criação (`app-pagina-creation-progress`): colar briefing → chat → preview da proposta → “Aplicar no editor”.
3. Após aplicar: preenche `titulo`, `slug`, `codigoTela`, `resumo`, `conteudoHtml`, `templateOrigem*`; usuário continua no fluxo normal (qualidade, anexos, enviar revisão).
4. Montagem preferencial: rail ao lado de `#pagina-revisao` / checklist de qualidade (assistente + qualidade na mesma coluna).

Espelhar UX do assistente de entrega (`EntregaWizardComponent`): passos claros, rascunho abandonável, ação final assíncrona com status.

---

## 8. Permissões e auditoria

MVP: **reutilizar** `PAGINA:CRIAR` / `PAGINA:EDITAR` / `PAGINA:LER` (sem proliferar catálogo).

Evolução (Fase B/C):

| Permissão | Uso |
|---|---|
| `PAGINA:AI_GERAR` | Abrir sessão / gerar |
| `PAGINA:AI_APLICAR` | Aceitar proposta |
| `PAGINA:AI_PROPOSTA` | Ver fila de propostas vindas de PR |

Auditoria: eventos `AI_SESSAO_CRIADA`, `AI_PROPOSTA_GERADA`, `AI_PROPOSTA_ACEITA`, `AI_PROPOSTA_REJEITADA` em `AuditoriaEvento` (mesmo append-only do DocFlow).

---

## 9. Fase B — ajustar página existente

> **Implementada** como ajuste por patch sobre o HTML atual: ver
> [`13-assistente-ia-fase-b.md`](13-assistente-ia-fase-b.md). O caminho é
> `POST /ai/paginas/{id}/ajustes`; `POST /ai/sessoes` com `ATUALIZAR_PAGINA` continua 422.

- `objetivo=ATUALIZAR_PAGINA` + `paginaId` obrigatório.
- Contexto do prompt inclui `conteudoHtml` atual (truncado por seções se grande).
- Proposta `tipo=ATUALIZACAO`: UI mostra **diff** reutilizando o comparador já existente em `pagina-revisoes` / central de revisão.
- Aplicar → `PUT /paginas/{id}` (cria `PaginaRevisao` `SALVAMENTO_MANUAL`) ou autosave se ainda `RASCUNHO`.

---

## 10. Fase C — PR merged → proposta

### 10.1 Gatilho

`POST /api/v1/docflow/webhooks/github`  
Auth: header `X-Webhook-Secret` (padrão `JenkinsWebhookController`).  
Security: `permitAll` só nesse path; secret obrigatório em prod.

Eventos aceitos (inicial):

- `pull_request` com `action=closed` + `merged=true` + base `main` (ou branches configuráveis).
- Opcional: label `docs:update` para filtrar ruído.

### 10.2 Pipeline

```text
Webhook → AiIngestaoPrService
  1. Persistir evento (idempotência por delivery-id)
  2. Buscar arquivos do PR (estender cliente GitHub: pulls + files + patch)
  3. Classificar: UI_NOVA | UI_ALTERACAO | SO_BACKEND | IRRELEVANTE
  4. Se irrelevante → fim
  5. Extrair sinais: codigoTela, rotas, labels, componentes de tela
  6. Match em tb_pagina por codigoTela / slug / título
  7. Abrir AiSessao (briefing = resumo do PR + trechos) + Job GERAR_RASCUNHO
  8. AiProposta PENDENTE na fila editorial
```

### 10.3 Fila na UI

Nova rota sugerida: `/doc-flow/propostas-ia`  
Lista propostas `PENDENTE` com origem `PR`, link para diff e ações Aceitar / Rejeitar / Abrir no editor.

### 10.4 GitHub — o que falta hoje

`GitHubReleasesAdapter` (release-orchestrator) tem releases, compare entre refs e download de arquivo. **Não** tem Pull Requests.

Fase C precisa de cliente mínimo:

- `GET /repos/{owner}/{repo}/pulls/{n}`
- `GET /repos/{owner}/{repo}/pulls/{n}/files`
- (opcional) contents por path no SHA do merge

Credencial: token por produto/projeto DocFlow (espelhar ideia de `ProdutoRh.githubToken`) ou secret global `DOCFLOW_GITHUB_TOKEN` no MVP.

---

## 11. Critérios de aceite (Fase A)

- [x] Feature flag `nexus.ai.enabled` liga/desliga UI e API.
- [x] Usuário cola briefing ≥ N caracteres e recebe perguntas **ou** proposta em &lt; ~90s (job async + SSE).
- [x] Proposta preenche o `pagina-form` sem quebrar autosave / `version`.
- [x] HTML gerado passa sanitização; não introduz `<script>` nem `javascript:` links.
- [x] Checklist de qualidade roda; erros bloqueantes são visíveis antes de “enviar revisão”.
- [x] Página materializada nasce como `RASCUNHO` (se modo persistir) e segue workflow atual.
- [x] Testes unitários do orquestrador com `LlmProvider` fake (contrato JSON).
- [x] Teste de serviço: aplicar proposta → `Pagina` com `templateOrigem*` quando houver template.
- [x] Sem chave configurada → provider fake + UI/status claros (sem stacktrace); `enabled=false` → 503.

**Smoke local (2026-08-03):** `GET /status` (fake) → sessão `PRONTA_PARA_GERAR` → `POST /gerar` → proposta com `doc-intro`/`screen-placeholder` → `aplicar FORM` → auditoria `AI_SESSAO_CRIADA` + `AI_PROPOSTA_GERADA` + `AI_PROPOSTA_APLICADA_FORM`. OpenRouter real exige `NEXUS_AI_API_KEY` no ambiente.

---

## 12. Plano de implementação sugerido

Checklist detalhado com tickets `AI-xxx`: [`11-assistente-ia-checklist-sprints.md`](11-assistente-ia-checklist-sprints.md).

| Sprint | Entrega |
|---|---|
| S1 | Properties + `LlmProvider` + entities/migrations + `POST /ai/sessoes` + triagem perguntas (pode ser heurística + LLM) |
| S2 | Job async + SSE + geração HTML a partir de template + sanitização + qualidade |
| S3 | UI drawer no `pagina-form` + aplicar no form + flags/permissões |
| S4 | Métricas/auditoria + polish prompts + testes e2e “briefing → rascunho no editor” |
| S5+ | Fase B (diff update) |
| S6+ | Fase C (webhook + fila propostas) |

---

## 13. Riscos e mitigações

| Risco | Mitigação |
|---|---|
| HTML fora do design system | Gerar sempre sobre esqueleto de template; whitelist de classes |
| Alucinação de `codigoTela` | Pergunta obrigatória ou match contra catálogo existente |
| Custo/latência LLM | Job async, modelo “mini” default, limite de tokens, cache de triagem |
| Publicação acidental | Sem endpoint de publicar na camada AI; só `RASCUNHO`/proposta |
| Segredo vazando em log | Nunca logar `api-key`; mascarar tokens no audit payload |
| Acoplamento RO ↔ DocFlow | Cliente GitHub próprio no DocFlow na Fase C |

---

## 14. Decisões travadas

1. Humano no loop — IA não publica.
2. Módulo Maven `nexus-ai` (extraível); UI Angular embutida no DocFlow (`/doc-flow/assistente`).
3. Jobs no padrão Publicação (`@Async` + SSE).
4. Templates como âncora do HTML gerado.
5. Match futuro PR↔página por `codigoTela`.
6. Secrets só via env / `@ConfigurationProperties` (`nexus.ai.*`).
7. Fase A antes de webhook.
8. Integração DocFlow só em `ai.integration.docflow`.

---

## 15. Referências no código atual

| Peça | Caminho |
|---|---|
| Entidade página | `docflow/.../entity/Pagina.java` |
| Templates aplicar | `PaginaTemplateService.aplicar` |
| Qualidade | `PaginaQualidadeService.avaliar` |
| Job async referência | `PublicacaoWorkerService` + `PublicacaoEventService` |
| Webhook referência | `JenkinsWebhookController` |
| Editor UI | `pagina-form` + `PaginaRichEditorComponent` |
| CSS canônico | `frontend/src/styles/components/_doc-content.css` |
| Seeds de template | `V10__docflow__04_seed_templates.sql` |
| Assistente UX irmão | `docs/release-orchestrator/18-nova-entrega-assistente.md` |
