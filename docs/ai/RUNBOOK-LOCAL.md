# Nexus AI — Runbook local (15 min)

Subir o assistente de páginas em localhost com Fake LLM (sem chave) ou OpenRouter.

## 1. Pré-requisitos

- API + Postgres já rodando (perfil `dev`) — ver [`../jornadas/09-passo-a-passo-local-tela-a-tela.md`](../jornadas/09-passo-a-passo-local-tela-a-tela.md)
- Front Angular (`npm start` em `nexus-portal-web/frontend`)

## 2. Variáveis (`NEXUS_AI_*`)

| Env | Default (dev) | Descrição |
|---|---|---|
| `NEXUS_AI_ENABLED` | `true` | Liga o módulo |
| `NEXUS_AI_API_KEY` | vazio | Sem chave → `FakeLlmProvider` (em `prod` com AI ligada, o boot falha) |
| `NEXUS_AI_BASE_URL` | `https://openrouter.ai/api/v1` | OpenRouter |
| `NEXUS_AI_MODEL` | `openai/gpt-5.6-luna` | Slug OpenRouter |
| `NEXUS_AI_REASONING_EFFORT` | `low` | Esforço de raciocínio do GPT-5.6 |
| `NEXUS_AI_HTTP_REFERER` | `http://localhost:4200` | Attribution |
| `NEXUS_AI_APP_TITLE` | `Nexus AI` | Attribution |
| `NEXUS_AI_MAX_GERACOES_POR_HORA` | `20` | Rate limit → 429 |

```bash
export NEXUS_AI_ENABLED=true
# opcional (geração real):
export NEXUS_AI_API_KEY=sk-or-...
```

## 3. Verificar

```bash
# login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq -r .token)

curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/ai/status | jq
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/docflow/paginas/blocos | jq 'length'
```

Esperado: `"enabled": true` e `45` blocos. Sem key: `"provider": "fake"`,
`"prontoParaGerar": false` (triagem/geração fake ainda funcionam).

Subir API (com Postgres já rodando):

```bash
./mvnw -pl application -am install -DskipTests
NEXUS_AI_ENABLED=true ./mvnw -pl application spring-boot:run -Dspring-boot.run.profiles=dev
```

## 4. UI

1. Login → DocFlow → Páginas → **Criar com IA** (ou `/doc-flow/assistente`)
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
| `404` em `http://localhost:4200/api/ai/status` | Inicie com `npm start`/`ng serve` para carregar `proxy.conf.json`; confirme que a API está na porta 8080 |
| `/paginas/blocos` retorna `400` ou a UI mostra biblioteca indisponível | A API ainda está com classes antigas. Recompile e reinicie o processo Java |
| O modelo sugerido aparece para confirmação | A confiança ficou abaixo de 70%; selecione uma sugestão ou force um modelo em **Avançado** |
| 503 nas APIs AI | `NEXUS_AI_ENABLED=true` + restart |
| 429 | Aumente `NEXUS_AI_MAX_GERACOES_POR_HORA` ou aguarde 1h |
| HTML pobre | Configure `NEXUS_AI_API_KEY` (sai do Fake) |

Nunca coloque a chave no YAML ou no Git. Use `NEXUS_AI_API_KEY` no ambiente e
revogue imediatamente qualquer chave que tenha aparecido em arquivo, log ou histórico.
