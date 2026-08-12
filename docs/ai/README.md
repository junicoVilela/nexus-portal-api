# Nexus AI — Backend

Módulo Maven **`ai/`** (`nexus-ai`). Assistente de IA do portal: sessões, geração de rascunhos de página DocFlow e (futuro) propostas a partir de PR.

> Spec de desenho: [`../doc-flow/10-assistente-ia-paginas.md`](../doc-flow/10-assistente-ia-paginas.md)  
> Checklist sprints: [`../doc-flow/11-assistente-ia-checklist-sprints.md`](../doc-flow/11-assistente-ia-checklist-sprints.md)  
> Frontend: `nexus-portal-web/docs/ai/`

---

## Por que módulo separado

| Benefício | Como |
|---|---|
| Extração futura | API própria `/api/v1/ai/**`, schema `V*__ai__*`, config `nexus.ai.*` |
| Fronteira DocFlow | Só em `com.nexus.portal.ai.integration.docflow` → troca por HTTP |
| Deploy atual | Embarcado no `application` (mesmo processo/porta) |

---

## Localização

| Item | Caminho |
|---|---|
| Código | `nexus-portal-api/ai/src/main/java/com/nexus/portal/ai/` |
| Migrations | `application/.../db/migration/V*__ai__*.sql` (centralizadas) |
| Config | `nexus.ai.*` / env `NEXUS_AI_*` |

Pacote base: `com.nexus.portal.ai.{config|controller|dto|entity|repository|service|provider|integration}`

---

## Prefixo da API

```text
/api/v1/ai/{recurso}
```

| Endpoint | Auth | Status |
|---|---|---|
| `GET /api/v1/ai/status` | `PAGINA:LER` | ✅ |
| `POST /api/v1/ai/templates/recomendacao` | `PAGINA:LER` | ✅ ranking + confiança |
| `POST /api/v1/ai/sessoes` | `PAGINA:CRIAR` | ✅ S1 |
| `GET /api/v1/ai/sessoes/{id}` | `PAGINA:LER` | ✅ S1 |
| `POST /api/v1/ai/sessoes/{id}/mensagens` | `PAGINA:CRIAR` ou `EDITAR` | ✅ S1 |
| `POST /api/v1/ai/sessoes/{id}/cancelar` | `PAGINA:EDITAR` | ✅ S1 |
| `POST /api/v1/ai/sessoes/{id}/gerar` | `PAGINA:CRIAR` | ✅ S2 (202) |
| `GET /api/v1/ai/sessoes/{id}/proposta` | `PAGINA:LER` | ✅ S2 |
| `POST /api/v1/ai/sessoes/{id}/aplicar` | `PAGINA:CRIAR` | ✅ S2 FORM/PERSISTIR |
| `GET /api/v1/ai/eventos` | `PAGINA:LER` | ✅ SSE |
| `GET /api/v1/docflow/paginas/blocos` | `PAGINA:LER` | ✅ catálogo canônico |
| `GET /api/v1/docflow/paginas/blueprints` | `PAGINA:LER` | ✅ receitas editoriais |
| `GET /api/v1/docflow/paginas/biblioteca` | `PAGINA:LER` | ✅ snapshot composicional v1 |

Proxy front: `/api/ai` → `/api/v1/ai`.

---

## Geração orientada pelo catálogo

O fluxo não pede HTML livre ao modelo:

1. `AiTemplateSelector` ranqueia os templates da biblioteca e calcula confiança.
2. Com confiança `>= 0,70`, o template é escolhido automaticamente; abaixo disso, a UI pede
   confirmação e mantém o seletor manual em **Avançado**.
3. O código do template resolve um blueprint editorial. Dez blueprints declarativos cobrem os
   vinte templates de sistema e classificam suas seções como obrigatórias, recomendadas ou
   opcionais.
4. `AiComponenteRetriever` seleciona a menor composição aplicável: mantém a base do blueprint,
   escolhe alternativas pelo briefing e só inclui opcionais quando houver evidência textual.
5. O provedor recebe somente o blueprint, IDs, descrições e slots permitidos e devolve uma
   `PageSpec` em JSON Schema.
6. O servidor valida IDs/slots e `PaginaBlocoCatalogoService` renderiza o HTML confiável.
7. A `PageSpec` v2 persiste `schemaVersion` e `blueprintId` em
   `tb_ai_proposta.page_spec_json` para auditoria e reprodução.

Isso separa decisão editorial de renderização: o modelo escreve textos, mas não inventa DOM,
classes, scripts ou componentes.

O catálogo fica em
`docflow/src/main/resources/docflow/pagina-blocos.json`; editor e IA consomem a mesma fonte.
Ao adicionar um bloco, atualize esse arquivo e seus testes — não crie uma cópia no Angular.
As composições ficam em `docflow/src/main/resources/docflow/pagina-blueprints.json` e sempre
referenciam componentes existentes, sem duplicar HTML.

### Quando adicionar RAG vetorial

A recuperação atual é híbrida determinística (template + metadados), adequada para dezenas de
componentes. Adicione embeddings/pgvector atrás do contrato de `AiComponenteRetriever` quando
houver centenas de blocos, documentos de domínio extensos ou métricas reais de baixa cobertura.
Mesmo com RAG, mantenha a `PageSpec`, o allowlist de componentes e a renderização no servidor.

---

## Configuração (OpenRouter)

Provider padrão: [OpenRouter](https://openrouter.ai/docs/quickstart) (`https://openrouter.ai/api/v1`), API compatível com OpenAI Chat Completions.

```bash
export NEXUS_AI_ENABLED=true
export OPENROUTER_API_KEY=sk-or-...   # ou NEXUS_AI_API_KEY
export NEXUS_AI_MODEL=openai/gpt-4o-mini
export NEXUS_AI_HTTP_REFERER=http://localhost:4200
export NEXUS_AI_APP_TITLE=Nexus AI
```

```yaml
nexus:
  ai:
    enabled: true
    base-url: https://openrouter.ai/api/v1
    api-key: ${NEXUS_AI_API_KEY:${OPENROUTER_API_KEY:}}
    model: openai/gpt-4o-mini
    http-referer: http://localhost:4200
    app-title: Nexus AI
    timeout-seconds: 90
    max-perguntas: 5
    max-tokens-saida: 8000
    max-geracoes-por-hora: 20
```

Headers enviados ao OpenRouter: `Authorization`, `HTTP-Referer`, `X-Title`, `X-OpenRouter-Title`.

Sem chave ou `enabled=false` → `FakeLlmProvider` (dev/test seguro).

Runbook local: [`RUNBOOK-LOCAL.md`](RUNBOOK-LOCAL.md).

---

## Extração para serviço

1. Promover `ai/` + migrations `V*__ai__*` para repo/serviço próprio.
2. Trocar `integration.docflow` por cliente HTTP DocFlow.
3. Front já usa `environment.aiApiUrl` — apontar para o host do serviço.
4. Auth: reutilizar JWT do portal ou gateway.

---

## Estado atual

- [x] Módulo Maven `nexus-ai` no parent + dependency no `application`
- [x] `AiProperties` + `LlmProvider` (fake + OpenAI-compatible / OpenRouter)
- [x] `GET /status`
- [x] Sessões + triagem heurística (S1) — `V16__ai__01_tables.sql`
- [x] Jobs/propostas (S2) — `V17__ai__02_jobs_propostas.sql`
- [x] Catálogo canônico + PageSpec auditável — `V18__ai__03_page_spec.sql`
- [x] Seleção automática com confiança e confirmação humana
- [x] Wizard UI + aplicar no editor (S3)
- [x] Hardening: auditoria, métricas, rate limit, prompts, e2e (S4)
- [ ] Atualizar página (S5)
- [ ] Webhook PR (S6)
