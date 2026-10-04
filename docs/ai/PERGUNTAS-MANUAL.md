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
| Sistema do cliente | `help-bridge.js` → `NexusManual.perguntar(...)` | Manual vigente do cliente |

O "manual vigente" é a última publicação concluída do cliente.

## Conectar um agente (MCP)

Na aba **Perguntar** da publicação, clique em **Gerar comando de conexão** e copie o comando:

```bash
claude mcp add --transport http manual-acme https://<portal>/api/doc-flow/mcp \
  --header "Authorization: Bearer <chave do manual nxm_… ou link de prévia>"
```

Ferramentas expostas:

| Ferramenta | O que faz |
|---|---|
| `buscar` | Devolve os trechos mais relevantes, com código da tela e caminho |
| `paginaPorCodigo` | Devolve a página inteira de uma tela, em Markdown |
| `listarTelas` | Lista código, título e caminho de todas as telas |

O transporte é Streamable HTTP sem streaming: cada POST é uma mensagem JSON-RPC.

## Credencial de leitura

Use uma **chave do manual** (`nxm_…`), criada em Configurações → "Manual nos sistemas do
cliente":

- tem nome, origens permitidas (CORS) e validade opcional; pode ficar sem expiração e ser
  revogada a qualquer momento;
- só aparece na criação, e o banco guarda o hash;
- tem limite próprio de acessos: `docflow.manual.limite-por-minuto`, padrão 600 por minuto.

O link de prévia do cliente também é aceito, mas vale 72 h e aceita 30 acessos por minuto. Serve
para testes, não para integrações fixas.

## Manual dentro do sistema do cliente (Onda D)

```html
<script src="https://<portal>/api/v1/manual/help-bridge.js" data-token="nxm_..."></script>
<script>
  // botão de ajuda da tela:
  NexusManual.open({ codigoTela: 'PED-001' });
  const r = await NexusManual.perguntar('como filtrar pedidos?');
</script>
```

| Rota (`/api/v1/manual/{chave}/…`) | O que devolve |
|---|---|
| `vigente` | Cliente, versão e data do manual vigente |
| `tela/{codigoTela}` | Título, caminho e `url` da tela; 404 com mensagem se a tela não estiver no manual |
| `site/**` | O ZIP publicado servido arquivo a arquivo (`site/index.html?tela=PED-001`) |

Com o ZIP descompactado ao lado do sistema, use
`NexusManual.configure({ localUrl: 'file:///…/index.html' })`. O `help-bridge.js` também vai no
pacote, em `assets/`.

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
