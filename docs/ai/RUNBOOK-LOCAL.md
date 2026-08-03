# Nexus AI — Runbook local (15 min)

Subir o assistente de páginas em localhost com Fake LLM (sem chave) ou OpenRouter.

## 1. Pré-requisitos

- API + Postgres já rodando (perfil `dev`) — ver [`../jornadas/09-passo-a-passo-local-tela-a-tela.md`](../jornadas/09-passo-a-passo-local-tela-a-tela.md)
- Front Angular (`npm start` em `nexus-portal-web/frontend`)

## 2. Variáveis (`NEXUS_AI_*`)

| Env | Default (dev) | Descrição |
|---|---|---|
| `NEXUS_AI_ENABLED` | `true` | Liga o módulo |
| `NEXUS_AI_API_KEY` / `OPENROUTER_API_KEY` | vazio | Sem chave → `FakeLlmProvider` |
| `NEXUS_AI_BASE_URL` | `https://openrouter.ai/api/v1` | OpenRouter |
| `NEXUS_AI_MODEL` | `openai/gpt-4o-mini` | Slug OpenRouter |
| `NEXUS_AI_HTTP_REFERER` | `http://localhost:4200` | Attribution |
| `NEXUS_AI_APP_TITLE` | `Nexus AI` | Attribution |
| `NEXUS_AI_MAX_GERACOES_POR_HORA` | `20` | Rate limit → 429 |

```bash
export NEXUS_AI_ENABLED=true
# opcional (geração real):
export OPENROUTER_API_KEY=sk-or-...
```

## 3. Verificar

```bash
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/ai/status | jq
```

Esperado: `"enabled": true`. Sem key: `"provider": "fake"`, `"prontoParaGerar": false` (triagem ainda funciona).

## 4. UI

1. Login → DocFlow → Páginas → **Criar com IA** (ou `/ai/assistente`)
2. Cole briefing (≥ 40 chars; inclua `codigoTela` para pular perguntas)
3. **Gerar rascunho** → revisar → **Aplicar no editor**
4. Salve o rascunho manualmente (IA nunca publica)

## 5. Auditoria / métricas

- Eventos `AI_SESSAO_*` / `AI_PROPOSTA_*` em Segurança → Auditoria (`entidade=AI_SESSAO` ou `AI_PROPOSTA`)
- Logs: `ai.job.completed ... latencyMs=... tokensIn=... tokensOut=...`

## 6. Problemas comuns

| Sintoma | Ação |
|---|---|
| CTA “Criar com IA” sumiu | `GET /ai/status` com `enabled=false` |
| 503 nas APIs AI | `NEXUS_AI_ENABLED=true` + restart |
| 429 | Aumente `NEXUS_AI_MAX_GERACOES_POR_HORA` ou aguarde 1h |
| HTML pobre | Configure `OPENROUTER_API_KEY` (sai do Fake) |
