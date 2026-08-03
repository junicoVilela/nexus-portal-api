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
| `POST /api/v1/ai/sessoes` | `PAGINA:CRIAR` | ✅ S1 |
| `GET /api/v1/ai/sessoes/{id}` | `PAGINA:LER` | ✅ S1 |
| `POST /api/v1/ai/sessoes/{id}/mensagens` | `PAGINA:CRIAR` ou `EDITAR` | ✅ S1 |
| `POST /api/v1/ai/sessoes/{id}/cancelar` | `PAGINA:EDITAR` | ✅ S1 |
| `POST /api/v1/ai/sessoes/{id}/gerar` | `PAGINA:CRIAR` | ✅ S2 (202) |
| `GET /api/v1/ai/sessoes/{id}/proposta` | `PAGINA:LER` | ✅ S2 |
| `POST /api/v1/ai/sessoes/{id}/aplicar` | `PAGINA:CRIAR` | ✅ S2 FORM/PERSISTIR |
| `GET /api/v1/ai/eventos` | `PAGINA:LER` | ✅ SSE |

Proxy front: `/api/ai` → `/api/v1/ai`.

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
- [x] Wizard UI + aplicar no editor (S3)
- [x] Hardening: auditoria, métricas, rate limit, prompts, e2e (S4)
- [ ] Atualizar página (S5)
- [ ] Webhook PR (S6)
