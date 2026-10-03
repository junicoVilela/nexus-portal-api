# Prompts do Nexus AI

Cada arquivo `<nome>.md` é um prompt carregado por `AiPromptCatalogo`. Para ajustar o
comportamento da IA, edite o texto aqui. Não precisa mexer em Java.

## Formato

```text
---
versao: 3
---
Texto do prompt com {{variavel}}.
```

- **Suba `versao` a cada mudança de texto.** O par `nome@versao` fica gravado em cada
  proposta (`tb_ai_proposta.prompt_versao`) e no log `ai.job.completed`. É assim que dá para
  comparar a taxa de aceite e rejeição entre versões.
- `{{variavel}}` é preenchida pelo código. Variável sem valor ou placeholder desconhecido
  faz a renderização falhar, e o `AiPromptCatalogoTest` pega isso no build.
- O conteúdo do autor (briefing, manifesto) vai sempre entre marcas `<<<…` / `…>>>`. O
  system prompt manda tratar esse trecho como dado e não como instrução. Mantenha as marcas.

## Prompts

| Arquivo | Uso | Variáveis |
|---|---|---|
| `gerar-page-spec.system.md` | Geração de página (system) | — |
| `gerar-page-spec.user.md` | Geração de página (user) | `templateCodigo`, `templateNome`, `titulo`, `codigoTela`, `resumo`, `briefing`, `contexto`, `blueprint`, `catalogo`, `ajustes` |
| `analise-documento.system.md` | Organização de documento importado | — |
| `analise-documento.user.md` | idem | `arquivo`, `projetoNome`, `manifesto` |
| `analise-documento-amplo.system.md` | Organização de documento grande (só módulos) | — |
| `analise-documento-amplo.user.md` | idem | `arquivo`, `projetoNome`, `manifesto` |
| `ajustar-pagina.system.md` | Ajuste de página existente (Fase B) | — |
| `ajustar-pagina.user.md` | idem | `titulo`, `resumo`, `escopo`, `pedidos`, `esboco`, `catalogo`, `propostaAnterior` |

O `FakeLlmProvider` (dev/test) reconhece os marcadores `TAREFA=GERAR_PAGE_SPEC`,
`tituloSugerido:`, `codigoTelaSugerido:`, `resumoSugerido:` e as linhas `- <componenteId> |`
do catálogo. Mantenha esses marcadores ao editar `gerar-page-spec.user.md`.
