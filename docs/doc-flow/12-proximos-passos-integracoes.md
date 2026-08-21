# 12 — Próximos passos: integrações DocFlow

Plano executável para integrar no DocFlow o que o mercado de docs (2026) já trata como padrão, **sem** virar outro Notion/Mintlify.

> Origem: análise do módulo + backlog AI S5–S7 (`11`) + o que vale copiar de GitBook, Mintlify, kapa.ai, Scribe/HappyRecorder, Loop, Beacon.  
> Frontend: `nexus-portal-web/docs/docflow/`  
> IA já desenhada: [`11-assistente-ia-checklist-sprints.md`](11-assistente-ia-checklist-sprints.md) (S5/S6 entram neste plano como ondas B e C).

**Legenda:** `[ ]` pendente · `[x]` concluído · **API** backend · **WEB** frontend · **PKG** gerador de pacote · **OPS** config/infra  
**Duração sugerida:** ~1 semana por onda (ajustar ao time). Ondas A e B podem paralelizar; C depende de B estar usável; D+ exigem A.

Princípio duro (não negociar): a IA **nunca publica**. Corpus de agente/chat do leitor = **snapshot da publicação**, não o rascunho editorial.

---

## Visão rápida

| Onda | Foco | Inspiração de mercado | Saída verificável | Depende de |
|---|---|---|---|---|
| **A** | Pacote AI-ready + deep link | GitBook/Mintlify `llms.txt` | ZIP com `llms.txt` + PWA `?tela=` | — |
| **B** | Chat no editor (AI S5) | Gemini Docs / Notion | “reescreva X” → diff → rascunho | S4 ✅ |
| **C** | PR/release → fila (AI S6) | GitBook AI Agent | Merge UI → item em `/propostas-ia` | B |
| **D** | Manual no produto | Help Scout Beacon | Widget/URL resolve `codigo_tela` | A |
| **E** | Answer engine + MCP | kapa.ai, GitBook MCP | Chat citado + MCP autenticado | D |
| **F** | Captura, snippet, gap | Scribe/HappyRecorder, Loop | Print ligado à tela; busca do PWA logada | A |

Fora deste plano (não abrir ticket): segundo editor, Git sync bidirecional, playground OpenAPI, i18n do admin, RAG no catálogo de blocos, publicação automática, tema white-label sofisticado.

---

## O que já existe (não recriar)

O gerador de pacote (`GeradorPacoteService`) já grava:

| Arquivo | Uso agora | Uso nas ondas |
|---|---|---|
| `routes.json` | mapa `codigoTela → paginas/{slug}.html` | A (`?tela=`), D (widget), E (MCP) |
| `search-index.json` | busca do PWA | E (retrieval), F (analytics) |
| `manifest.json` | cliente, versão, tema | D/E (qual snapshot está no ar) |

`codigo_tela` é único na página. `DocFlowAiBridge` é a única ponte AI ↔ DocFlow. Tickets S5/S6 já numerados `AI-5xx` / `AI-6xx` — este doc **não os renumera**.

---

## Onda A — Pacote AI-ready + deep link

**Objetivo:** o ZIP/PWA fala a língua de 2026 (agentes + URL estável por tela) sem hospedar site ainda.

**Demo:** abrir o `index.html` do pacote com `?tela=CODIGO` cai na página certa; `llms.txt` lista títulos e códigos; `llms-full.txt` contém o texto das páginas.

### Tickets

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| INT-101 | PKG | Emitir `llms.txt` na raiz do ZIP | Markdown: H1 do manual, lista `- [titulo](url): codigoTela — resumo` |
| INT-102 | PKG | Emitir `llms-full.txt` | Uma seção por página (`# titulo`, código, texto plano do HTML) |
| INT-103 | PKG | Incluir os dois arquivos no relatório de validação do pacote | `PublicacaoDownloadIntegrationTest` falha se ausentes |
| INT-104 | PKG | PWA: `?tela=` / `#tela=` resolve via `routes.json` | Código inexistente mostra empty state, não 404 mudo |
| INT-105 | PKG | `routes.json` e `manifest.json` já copiados para `assets` acessíveis no preview HTML | Preview autenticado também honra `?tela=` |
| INT-106 | API+WEB | Detalhe da publicação: copiar “link da tela” | Combina URL de preview/token + `?tela=` |
| INT-107 | API | Teste unitário `GeradorPacoteService` cobre `llms*.txt` + rota por código | CI verde |

### Checklist fino

- [ ] INT-101 `llms.txt`
- [ ] INT-102 `llms-full.txt`
- [ ] INT-103 validação do ZIP
- [ ] INT-104 deep link PWA
- [ ] INT-105 preview
- [ ] INT-106 copiar link no detalhe
- [ ] INT-107 testes

**Não fazer nesta onda:** MCP, hospedagem CDN, chat no PWA.

---

## Onda B — Chat no editor (AI S5)

**Objetivo:** a IA vira ferramenta diária da editora, não só wizard de página nova.

Reusar tickets de [`11-assistente-ia-checklist-sprints.md`](11-assistente-ia-checklist-sprints.md) § Sprint 5. Critério extra deste plano: o apply **nunca** muda `PUBLICADO` direto — só rascunho/autosave/atualizar com revisão.

| ID | Status neste plano |
|---|---|
| AI-501 … AI-507 | Executar na íntegra |

**Demo:** página aberta → “reescreva pré-requisitos” → diff lado a lado (componente de revisões) → aceitar → `version` incrementa, status permanece o de edição.

**Não fazer nesta onda:** webhook GitHub, permissões `AI_*` (S7).

---

## Onda C — PR / release → fila (AI S6)

**Objetivo:** documentar deixa de ser memória da Lucia e vira efeito do merge/release.

Reusar Sprint 6 do `11`. Extensão DocFlow (não está no `11`):

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| AI-601 … AI-612 | — | Webhook GitHub + classificador + fila | Conforme `11` |
| INT-301 | API | Gancho Orchestrator: release `PUBLICADA` lista `codigo_tela` candidatos | Evento interno ou job; sem GitHub ainda funciona com payload fixture |
| INT-302 | API | Página marcada `desatualizadaPorReleaseId` (opcional, nullable) | Dashboard conta “obsoletas pela release”, não só 180 dias |
| INT-303 | WEB | Fila `/propostas-ia` mostra origem `PR` vs `RELEASE` | Filtro + link do PR/release |

C depende de B: aceitar proposta de atualização usa o mesmo apply/diff.

**Demo:** payload fixture de PR (ou release 1.5.0) → item na fila → aceitar → rascunho no editor.

**Não fazer nesta onda:** cliente único Orchestrator↔DocFlow (ver Débitos de plataforma).

---

## Onda D — Manual no produto

**Objetivo:** o NEXUS-LD (ou qualquer app interno) abre o tópico da tela atual, no recorte daquele cliente.

Pré-requisito: Onda A. Hospedagem mínima pode ser o preview token já existente, não precisa de CDN.

### Tickets

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| INT-401 | API | `GET /api/v1/docflow/manuais/{clienteSlug}/vigente` | Última publicação `SUCESSO` do cliente (escopo RBAC) |
| INT-402 | API | `GET .../vigente/tela/{codigoTela}` | 302/JSON `{ url, titulo, versao }` a partir do snapshot `routes.json` |
| INT-403 | API | Token de leitura do manual (reusa preview-token ou JWT de app) | App externo não usa cookie do portal |
| INT-404 | PKG | Script `help-bridge.js` no ZIP | `NexusManual.open({ codigoTela })` resolve local (file://) ou via API |
| INT-405 | WEB | Página de integração em Configurações: snippet + origem permitida | Copiar `<script>` + CORS allowlist |
| INT-406 | API+WEB | Teste: cliente sem página para o código → 404 de negócio, não 500 | Contrato estável para o app |

**Demo:** HTML estático do produto chama `open({ codigoTela: 'PED-CONSULTA' })` e cai na página do pacote/preview da ACME.

**Não fazer nesta onda:** chat RAG, MCP, multi-idioma.

---

## Onda E — Answer engine + MCP

**Objetivo:** pergunta do leitor/agente cai no **snapshot publicado** daquele cliente/versão, com citação.

Só abrir depois de D estar em uso (senão não há canal de leitor).

### Tickets

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| INT-501 | API | `POST /api/v1/docflow/manuais/{id}/perguntar` | Resposta grounded em `search-index` + HTML do ZIP; cita `url` + `codigoTela` |
| INT-502 | API | Guardrail “não sei” se score baixo | Não inventa passo que não está no snapshot |
| INT-503 | PKG | UI mínima no PWA: campo “Perguntar” (feature flag) | Fallback = busca atual se API off |
| INT-504 | API | MCP autenticado: tools `buscar`, `paginaPorCodigo` | Mesmo corpus da INT-501; token de INT-403 |
| INT-505 | API | Recusa corpus editorial (páginas `RASCUNHO`) | Teste explícito |
| INT-506 | API | Métricas: pergunta, hit/miss, página citada | Sem PII no log padrão |

Retrieval: híbrido determinístico (`codigo_tela` exact + keyword do `search-index`). **Não** adicionar pgvector nesta onda (ver `docs/ai/README.md` § RAG).

**Demo:** no PWA da v1.5.0 ACME, “como filtrar pedidos?” cita a página certa; pergunta sobre tela que o cliente não tem → “não sei”.

---

## Onda F — Captura, snippet, coverage gap

**Objetivo:** fechar o buraco que o checklist de qualidade já denuncia e o que o PWA já poderia medir.

### Tickets

| ID | Tipo | Título | Critério de pronto |
|---|---|---|---|
| INT-601 | API | Anexo de página guarda `codigoTela` (e seletor CSS opcional) | Qualidade/delta sabem qual print envelhece |
| INT-602 | WEB | Upload de mídia pede/herda código da página | Biblioteca global filtra por tela |
| INT-603 | OPS | Job Playwright **opcional** (env demo): abre tela, tira PNG, substitui `.screen-placeholder` | Humano confirma; sem LLM |
| INT-604 | API | Bloco transclusão `include` (snippet versionado) | Resolvido **na geração do pacote** (ZIP continua estático) |
| INT-605 | PKG | PWA envia evento de busca (query, zero hits, página aberta) | Mesmo padrão de retenção da ajuda (`docflow.ajuda.*`) |
| INT-606 | API+WEB | Dashboard: “buscas sem resultado” (30 dias) | Substitui parte do KPI de 180 dias |

**Demo:** print da tela PED-CONSULTA fica ligado ao código; release 1.5.0 (onda C) pode listar “prints possivelmente obsoletos”; busca vazia no PWA aparece no dashboard.

**Não fazer nesta onda:** regeneração autônoma de print a cada deploy (HappyRecorder completo). INT-603 é o teto.

---

## Débitos de plataforma (paralelos, não bloqueiam A–B)

Estes itens não são “feature de docs”, mas travam a jornada ACME. Tratar em PRs separados, sem misturar com INT-1xx.

| ID | Tema | Por quê |
|---|---|---|
| PLAT-01 | Cliente DocFlow ↔ Cliente Orchestrator | Sem ID compartilhado, widget e “o que ACME recebeu?” mentem |
| PLAT-02 | `tipoPagina=menu` no backend | Front já tem pasta; pacote mistura capítulo com artigo |
| PLAT-03 | `Cliente.definirTemas` ignora a cor informada | Schema de tema é teatro |
| PLAT-04 | Publicação DocFlow como artefato da entrega | ZIP técnico + manual na mesma história |
| PLAT-05 | Permissões `PAGINA:AI_*` (AI-704) | Depois que B/C tiverem uso real |

---

## Ordem de ataque (primeiro código)

1. **INT-101 → INT-104 → INT-107** (onda A visível num ZIP gerado hoje).
2. **AI-501 → AI-505 → AI-507** (onda B; reusa diff de revisões).
3. **AI-601 fixture → AI-606 → AI-610** (onda C mínima, GitHub real depois).
4. **INT-401 → INT-404** (onda D; preview token basta).
5. Só então E e F.

Ondas A e B podem andar em PRs paralelos (pacote vs módulo `ai/`).

---

## Fora de escopo (não criar ticket)

- Publicação automática pela IA
- Git sync bidirecional Markdown (source of truth continua o banco + workflow)
- Playground OpenAPI / portal estilo ReadMe
- RAG vetorial no catálogo de 45 blocos
- Docs Live / voz como coautor
- i18n do admin (`$localize` continua deferido)
- Signal Store / virtual scroll
- Segundo WYSIWYG ou CMS headless
- Substituição do ZIP: ele permanece o entregável air-gap; hospedagem é canal extra

---

## Como pedir no Cursor (onda A, primeiro PR)

```text
Implemente a Onda A de nexus-portal-api/docs/doc-flow/12-proximos-passos-integracoes.md
(tickets INT-101 a INT-107). Não avance S5/S6 nem MCP.

Regras: GeradorPacoteService já grava routes.json, search-index.json e manifest.json —
estenda escreverJson / validação do ZIP. Deep link ?tela= no PWA usa routes.json.
Testes em GeradorPacoteServiceTest + PublicacaoDownloadIntegrationTest.
IA nunca publica. Sem pgvector. Sem nova biblioteca se Jsoup/Jackson bastarem.
```

Onda B: “execute a Sprint 5 de `11-assistente-ia-checklist-sprints.md` com o critério extra da Onda B em `12` (apply não publica)”.

---

## Rastreio

| Doc | Papel |
|---|---|
| Este arquivo | Plano de integrações (ondas A–F) |
| [`11-assistente-ia-checklist-sprints.md`](11-assistente-ia-checklist-sprints.md) | Tickets AI-5xx / AI-6xx |
| [`10-assistente-ia-paginas.md`](10-assistente-ia-paginas.md) | Desenho da IA (humano no loop) |
| [`../ai/README.md`](../ai/README.md) | Quando (não) adicionar RAG |
| `GeradorPacoteService` | Contratos do pacote (`routes.json`, índice, PWA) |
| [`../jornadas/00-cenario-feliz-acme.md`](../jornadas/00-cenario-feliz-acme.md) | Jornada manual + release |
