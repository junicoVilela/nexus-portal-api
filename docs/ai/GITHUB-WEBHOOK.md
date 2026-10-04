# Fila de propostas a partir de PRs (Fase C)

Quando um PR que mexe em tela é mergeado, a IA prepara a documentação e o item aparece em
**Propostas da IA** (`/doc-flow/propostas-ia`). A IA nunca publica: quem tem `PAGINA:AI_PROPOSTA`
decide.

```text
GitHub (PR closed + merged na branch acompanhada)
  └─ POST /api/v1/ai/webhooks/github   assinatura HMAC (X-Hub-Signature-256)
       └─ tb_ai_pr_evento               uma vez por delivery e por PR
            └─ arquivos do PR (API do GitHub) → classificador → código de tela
                 ├─ página existe e está editável  → ajuste (patch da Fase B)
                 ├─ página aprovada/publicada      → aguardando rascunho
                 ├─ página não existe              → página nova no módulo do repositório
                 └─ só backend, docs ou testes     → ignorado
```

## 1. Configurar a API

| Variável | Para quê |
|---|---|
| `NEXUS_AI_GITHUB_WEBHOOK_SECRET` | O mesmo valor do campo *Secret* do webhook no GitHub. Sem ela, o endpoint responde 403. |
| `NEXUS_AI_GITHUB_TOKEN` | Leitura dos arquivos do PR. Obrigatório em repositório privado (token *fine-grained* com **Pull requests: read** e **Contents: read**). |
| `NEXUS_AI_GITHUB_REPOSITORIOS_0_NOME` | `org/repositorio` acompanhado. PR de repositório fora da lista fica registrado como ignorado. |
| `NEXUS_AI_GITHUB_REPOSITORIOS_0_PROJETO_ID` | Projeto DocFlow das páginas novas. |
| `NEXUS_AI_GITHUB_REPOSITORIOS_0_MODULO_ID` | Módulo onde a página nova nasce. Dá para trocar ao aceitar. |

Para mais repositórios, use os índices `_1_`, `_2_` e assim por diante. Opcionais, no YAML em
`nexus.ai.github`:

- `branches`: padrão `main`;
- `rotulo-ignorar`: padrão `docs:skip`;
- `caminhos-tela`: globs dos arquivos de tela;
- `max-arquivos`;
- `max-caracteres-patch`.

O módulo de IA precisa estar ligado (`NEXUS_AI_ENABLED=true` com `NEXUS_AI_API_KEY`). Os PRs que
chegarem com ele desligado ficam em erro e podem ser reprocessados depois.

## 2. Configurar o GitHub

No repositório, abra **Settings → Webhooks → Add webhook** e preencha:

- **Payload URL**: `https://<api>/api/v1/ai/webhooks/github`.
- **Content type**: `application/json`.
- **Secret**: o valor de `NEXUS_AI_GITHUB_WEBHOOK_SECRET`.
- **Events**: *Let me select individual events* → só **Pull requests**.

Depois de salvar, o GitHub envia um `ping`. Em *Recent Deliveries*, a resposta esperada é `204`.

| Resposta | Significado |
|---|---|
| `202` | PR mergeado aceito: o item aparece na fila em alguns segundos. |
| `204` | Evento sem efeito: ping, PR fechado sem merge, outra branch ou reentrega. |
| `403` | Secret não configurado na API ou diferente do configurado no GitHub. |

## 3. Como o PR é lido

- **Arquivos de tela** (padrão: `*.component.html|ts|css|scss`, `pages/`, `views/`, `*.tsx`,
  `*.jsx`, `*.vue`):
  - com arquivo novo, o PR vira *tela nova*;
  - só com arquivos modificados, vira *alteração de tela*.
- **Código da tela**: use `codigoTela: PED-001` (ou `tela: PED-001`) na descrição do PR. Sem
  marcador, a API tenta os códigos citados no título e na descrição. Um código que já existe no
  DocFlow liga o PR àquela página.
- **Pular a documentação**: coloque o rótulo `docs:skip` no PR.
- O briefing da IA leva o título, a descrição, a lista de arquivos de tela e trechos do diff.
  Uma boa descrição de PR gera uma boa página.

## 4. Na fila

| Situação | O que fazer |
|---|---|
| Página nova pendente | **Aceitar e criar rascunho**, **Abrir no editor** ou **Rejeitar**, com categoria. |
| Ajuste pendente | **Abrir no editor**: o painel *Ajustar com IA* abre com as mudanças propostas. |
| Aguardando rascunho | Volte a página para rascunho no editor e clique em **Gerar ajuste**. |
| Erro | Leia a mensagem (token, módulo desligado, limite de gerações) e clique em **Tentar de novo**. |

Ao agir num item, você o **assume**: a sessão da IA passa a ser sua, e regenerar ou ajustar
funciona como no assistente. O item registra o responsável. A auditoria grava `AI_PR_RECEBIDO` e
`AI_PR_ASSUMIDO`.

### Itens de release

Quando uma release vai para **PUBLICADA** no Release Orchestrator, cada tela citada na release
vira um item com origem **Release** e situação **Para revisar**. A IA não é chamada sozinha: o
item mostra o texto da release, quantas capturas da tela existem e dois botões, **Gerar ajuste
com IA** (como um PR) e **Dispensar: já está certa**. A página fica marcada como "alterada pela
release" até ser publicada de novo. O filtro *Origem* separa PRs e releases.

## 5. Testar sem o GitHub

```bash
SECRET=... ; BODY='{"action":"closed","repository":{"full_name":"org/app"},"pull_request":{"number":1,"title":"Tela PED-001","body":"codigoTela: PED-001","merged":true,"html_url":"https://github.com/org/app/pull/1","user":{"login":"dev"},"base":{"ref":"main"},"labels":[]}}'
SIG="sha256=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SECRET" | sed 's/^.* //')"
curl -i -X POST localhost:8080/api/v1/ai/webhooks/github \
  -H 'Content-Type: application/json' -H 'X-GitHub-Event: pull_request' \
  -H "X-GitHub-Delivery: teste-$(date +%s)" -H "X-Hub-Signature-256: $SIG" -d "$BODY"
```

A API ainda busca os arquivos no GitHub. Para um repositório fictício, o item cai em erro com a
mensagem do GitHub (404), o que já valida assinatura, registro e fila. O teste
`AiFilaPrIntegrationTest` cobre o caminho completo com o GitHub simulado.

## Onde fica cada coisa

| O quê | Onde |
|---|---|
| Webhook e assinatura | `AiGithubWebhookController`, `AiGithubAssinatura` |
| Classificação e código de tela | `AiPrClassificador` (regras no javadoc) |
| Processamento | `AiPrIngestaoService` |
| Fila (assumir, aceitar, rejeitar, reprocessar) | `AiFilaPrService`, `GET/POST /api/v1/ai/fila-pr` |
| Configuração | `AiGithubProperties` (`nexus.ai.github.*`) |
| Release publicada → fila | `ReleasePublicadaEvento`, `AiReleaseFilaListener`, `AiPrIngestaoService.processarRelease` |
