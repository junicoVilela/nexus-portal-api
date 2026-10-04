# Perguntar ao manual e MCP (Onda E)

O leitor ou um agente pergunta, e a resposta vem do **snapshot publicado**: o ZIP da publicação,
com a tela citada. Rascunhos e edições posteriores à publicação nunca entram.

```text
pergunta
  └─ corpus: rag/ do ZIP (um Markdown por tela, cortado nas seções ##)
       └─ busca: código de tela exato + BM25 por seção (ManualBusca)
            ├─ melhor trecho cobre < 50% dos termos → "não encontrei" (a IA nem é chamada)
            ├─ IA ligada   → resposta só com os trechos + citação validada (prompt responder-manual)
            └─ IA desligada ou falhou → os trechos, sem texto gerado
```

## Onde perguntar

| Canal | Como | Corpus |
|---|---|---|
| Portal | Publicação → aba **Perguntar** (`POST /api/v1/ai/publicacoes/{id}/perguntar`, `PUBLICACAO:LER`) | Aquela publicação |
| Leitor | Prévia online por token: caixa "Pergunte ao manual" (`POST /api/v1/ai/manual/{token}/perguntar`) | Manual vigente do cliente |
| Agente | MCP (`/api/v1/docflow/mcp`) | Manual vigente do cliente |

O "manual vigente" é a última publicação concluída do cliente.

## Conectar um agente (MCP)

Na aba **Perguntar** da publicação, clique em **Gerar comando de conexão** e copie o comando:

```bash
claude mcp add --transport http manual-acme https://<portal>/api/doc-flow/mcp \
  --header "Authorization: Bearer <token de prévia>"
```

Ferramentas expostas:

| Ferramenta | O que faz |
|---|---|
| `buscar` | Devolve os trechos mais relevantes, com código da tela e caminho |
| `paginaPorCodigo` | Devolve a página inteira de uma tela, em Markdown |
| `listarTelas` | Lista código, título e caminho de todas as telas |

O transporte é Streamable HTTP sem streaming: cada POST é uma mensagem JSON-RPC.

## Token de leitura

O token de prévia do cliente (Clientes → links de prévia) é a credencial de leitura do manual:

- vale 72 h por padrão;
- pode ser revogado a qualquer momento;
- é limitado por `docflow.preview.limite-por-minuto` (padrão: 30 acessos por minuto, somando
  prévia, perguntas e MCP).

Para integrações longas, gere tokens com validade maior ou renove-os pelo endpoint de
preview-tokens.

## Métricas

Cada pergunta fica em `tb_ai_manual_pergunta` com a pergunta, o modo (IA / TRECHOS / NAO_SEI) e as
telas citadas. Não guarda **quem** perguntou (usuário, IP ou token).

O painel **Qualidade da IA** mostra três coisas:

- a taxa de "não sei";
- as perguntas sem resposta, que são lacunas do manual e candidatas a página nova;
- as telas mais procuradas.

## Ajustar

| Quero… | Onde |
|---|---|
| Mudar quando a resposta é "não sei" | `ManualBusca.COBERTURA_MINIMA` |
| Mudar palavras ignoradas ou o radical | `ManualBusca` (`VAZIAS`, `SUFIXOS`) |
| Mudar o tom ou as regras da resposta | `prompts/responder-manual.*.md`; suba a `versao` (ver `PROMPT-OPS.md`) |
| Mudar quantos trechos vão para a IA | `AiManualPerguntaService.TRECHOS_PARA_IA` |
