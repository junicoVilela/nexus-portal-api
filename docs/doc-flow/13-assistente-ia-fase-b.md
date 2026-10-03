# 13 — Assistente de IA, Fase B: ajustar página existente

> Status: **implementado** (AI-501..506). Smoke manual (§15) pendente. Substitui o detalhamento da S5 em
> [`11-assistente-ia-checklist-sprints.md`](11-assistente-ia-checklist-sprints.md).
> Contexto: Fase A em produção. Ver [`10-assistente-ia-paginas.md`](10-assistente-ia-paginas.md) e
> [`../ai/README.md`](../ai/README.md).

---

## 1. Objetivo

No editor de uma página existente, o autor pede um ajuste em linguagem natural ("reescreva os
pré-requisitos para o perfil gestor", "deixe o passo a passo mais curto", "inclua um aviso sobre
o limite de exportação"). A IA propõe mudanças pontuais, o autor vê o diff, aceita tudo ou parte
e salva pelo fluxo normal do editor.

Princípios mantidos da Fase A:

- **A IA nunca publica e nunca salva sozinha.** O resultado vai para o editor. Quem salva é o autor,
  com a revisão `SALVAMENTO_MANUAL` de sempre.
- **O modelo não escreve HTML.** Ele devolve um patch estruturado. O servidor aplica, renderiza e
  sanitiza.
- **Humano decide por mudança, não só pelo pacote.**

Fora do escopo da Fase B: ajuste em lote de várias páginas, gatilho por PR (Fase C), geração de
imagens e reestruturação completa ("refaça a página do zero" continua sendo a Fase A).

---

## 2. Por que não regenerar a PageSpec

A Fase A gera a página a partir de uma PageSpec (componentes do catálogo + textos por slot). Para
ajustar páginas existentes, esse caminho não serve:

| Fato | Consequência |
|---|---|
| O HTML renderizado não guarda qual componente/slot gerou cada trecho | Não dá para reconstruir a PageSpec a partir da página salva |
| A maioria das páginas é editada à mão no WYSIWYG depois de criada | Mesmo páginas que nasceram da IA divergem da PageSpec original |
| Páginas têm imagens anexadas, tabelas, links e formatação manual | Regenerar perderia tudo isso |

Por isso a Fase B trabalha sobre **o HTML atual da página**, com edição pontual por unidade de texto.

---

## 3. Modelo de edição: patch sobre unidades de texto

### 3.1 Esboço da página (o que o modelo vê)

O servidor faz o parse do `conteudo_html` atual (Jsoup) e produz um **esboço**: seções numeradas,
cada uma com suas unidades de texto. O agrupamento em seções segue a mesma regra do
`pagina-section-organizer` do front (um título `h1–h6` abre uma seção; `section`/`article` é uma
seção inteira), para que o autor e o modelo enxerguem as mesmas seções.

```text
s1  "Consulta de pedidos" (section.doc-intro)
    u1  [h1]  Consulta de pedidos
    u2  [p]   Use esta tela para localizar pedidos por período e status.
s2  "Pré-requisitos"
    u3  [h2]  Pré-requisitos
    u4  [li]  Ter o perfil Operador.
    u5  [li]  Acesso ao módulo Vendas. 🔒 (contém link)
s3  "Passo a passo"
    ...
```

- **Unidade** = elemento de bloco com texto: `h1–h6`, `p`, `li`, `td`, `th`, `dt`, `dd`,
  `figcaption`, `blockquote`.
- Os ids (`s*`, `u*`) são posicionais e só valem para a **versão** da página em que o esboço foi
  gerado (ver §5, concorrência).
- **Unidades com marcação inline** (`a`, `img`, `code`, `strong`, `em`…) vão como **somente
  leitura** (🔒) na v1: substituir o texto apagaria links e formatação. O modelo pode citá-las,
  mas não alterá-las. Ver a decisão em aberto D2.
- Imagens, `screen-placeholder`, classes e atributos não aparecem no esboço e nunca são tocados.

### 3.2 Patch (o que o modelo devolve)

JSON Schema estrito, no mesmo padrão da PageSpec:

```json
{
  "resumoDaMudanca": "Pré-requisitos reescritos para o perfil gestor.",
  "operacoes": [
    { "id": "op1", "tipo": "ALTERAR_TEXTO", "unidadeId": "u4",
      "novoTexto": "Ter o perfil Gestor ou Operador com permissão PEDIDO:LER.",
      "motivo": "Pedido do autor: incluir o perfil gestor." },
    { "id": "op2", "tipo": "INSERIR_BLOCO", "aposSecaoId": "s3",
      "componenteId": "callout-atencao", "textos": [{ "slotId": "t1", "valor": "A exportação é limitada a 5.000 linhas." }],
      "motivo": "Pedido do autor: avisar o limite de exportação." },
    { "id": "op3", "tipo": "REMOVER_UNIDADE", "unidadeId": "u9",
      "motivo": "Duplica a informação do passo 2." }
  ]
}
```

| Tipo | Efeito no servidor | Limite |
|---|---|---|
| `ALTERAR_TEXTO` | Troca o texto da unidade, preservando a tag e os atributos | Só unidades editáveis (não 🔒) |
| `INSERIR_BLOCO` | Renderiza um componente do catálogo (`PaginaBlocoCatalogoService.renderizar`) e insere após a seção | Só componentes do catálogo, sem repetir o mesmo na mesma posição |
| `REMOVER_UNIDADE` | Remove o elemento da unidade | Nunca remove título de seção; o diff destaca em vermelho |

O título e o resumo da página podem vir como `ALTERAR_TEXTO` sobre as pseudo-unidades `titulo` e
`resumo`. O `codigoTela` e o `slug` não são alteráveis pela IA.

### 3.3 Aplicação

`AiPagePatchService.aplicar(htmlBase, operacoes)`:

1. Faz o parse de `htmlBase` e reconstrói o mapa `id → elemento` com a mesma travessia do esboço.
2. Aplica as operações aceitas em ordem estável: alterações, depois inserções, depois remoções.
   Assim os ids não se deslocam durante a aplicação.
3. Operação com id inexistente, unidade 🔒 ou componente fora do catálogo é **descartada** e vira
   um aviso (`avisos_geracao`, como na Fase A).
4. Sanitiza com `AiHtmlSanitizer` e roda o `PaginaQualidadeService` no resultado.

Se nenhuma operação sobreviver, a proposta é criada com o aviso "A IA não propôs mudanças
aplicáveis". Ela não vira erro, para o autor poder refinar a instrução.

---

## 4. Fluxo

```text
Editor (página aberta, sem alterações pendentes)
  │  "Ajustar com IA" → instrução + escopo (página inteira | seção sN)
  ▼
POST /api/v1/ai/paginas/{paginaId}/ajustes ─────────► 202 { sessaoId, job }
  │   cria AiSessao(ATUALIZAR_PAGINA, paginaId, versionBase) sem triagem
  │   e enfileira o job AJUSTAR (mesmo worker/SSE da Fase A)
  ▼
Worker: esboço → prompt ajustar-pagina → patch → aplicar → qualidade
  ▼
AiProposta(tipo=ATUALIZACAO, patch_json, version_base, conteudo_html=resultado)
  ▼
Painel no editor: lista de operações (aceitar/recusar cada uma) + diff lado a lado
  │  ├─ "Refinar" → POST /sessoes/{id}/gerar { instrucao }   (já existe)
  │  ├─ "Rejeitar" → POST /sessoes/{id}/proposta/rejeitar      (já existe)
  │  └─ "Aplicar selecionadas"
  ▼
POST /sessoes/{id}/aplicar { modo: FORM, operacoesAceitas: [op1, op3] }
  │   409 se a página mudou desde versionBase
  ▼
Editor recebe o HTML resultante → conteúdo marcado como alterado → autor salva normalmente
```

**O editor precisa estar sem alterações pendentes** para abrir o painel. Em rascunho, o autosave
garante isso antes de criar a sessão. Nos demais status, o painel pede para salvar primeiro. Assim o
esboço corresponde ao que o autor está vendo.

---

## 5. Concorrência

- A sessão grava `version_base` (a `@Version` da página no momento do pedido).
- `aplicar` relê a página: se `version != version_base`, responde **409** com "A página mudou desde
  a proposta. Gere novamente para trabalhar sobre a versão atual". Os ids do esboço só valem
  para a versão base, então reaplicar sobre outra versão seria inseguro.
- O worker também confere a versão antes de concluir, para não gerar proposta sobre base obsoleta.
- O salvamento final passa pelo `PUT /paginas/{id}` normal, com a mesma checagem de `version` e a
  mesma UI de conflito que o editor já tem.

---

## 6. API

### 6.1 Novo endpoint

| Método | Path | Auth | Papel |
|---|---|---|---|
| `POST` | `/api/v1/ai/paginas/{paginaId}/ajustes` | `PAGINA:EDITAR` | Cria a sessão de ajuste e enfileira o job → **202** |

```json
// request
{ "instrucao": "Reescreva os pré-requisitos para o perfil gestor.", "secaoId": "s2", "version": 7 }
// response 202
{ "sessaoId": "…", "job": { "id": "…", "status": "PENDENTE", "etapa": "AGUARDANDO" } }
```

- `instrucao`: 10–2.000 caracteres. `secaoId`: opcional, limita o esboço a uma seção.
  `version`: a versão que o editor tem; se a página já estiver em outra, a resposta é 409.
- Página `ARQUIVADO` → 422. Esboço acima do limite (§9) sem `secaoId` → 422 com "Escolha uma seção".

### 6.2 Endpoints reaproveitados

| Endpoint | Mudança |
|---|---|
| `POST /sessoes/{id}/gerar { instrucao }` | Nenhuma: o refinamento já leva a PageSpec anterior; para ATUALIZAR, leva o patch anterior |
| `POST /sessoes/{id}/proposta/rejeitar` | Nenhuma |
| `POST /sessoes/{id}/aplicar` | Aceita `operacoesAceitas: string[]` (ATUALIZAR, só modo `FORM`). Valida `version_base`. Reaplica o subconjunto sobre o HTML atual e devolve o HTML resultante |
| `GET /sessoes/{id}/proposta` | Passa a devolver `operacoes` (id, tipo, unidade, texto antes/depois, motivo) e `versionBase` |
| `POST /ai/sessoes` com `ATUALIZAR_PAGINA` | Continua 422: o caminho da Fase B é o endpoint por página |

`PERSISTIR` (salvar direto, sem passar pelo editor) fica **fora** da Fase B. Ver a decisão D1.

---

## 7. Modelo de dados (migration `V41__ai__13_ajuste_pagina.sql`)

| Tabela | Coluna | Tipo | Uso |
|---|---|---|---|
| `tb_ai_sessao` | `version_base` | `BIGINT NULL` | Versão da página quando o ajuste foi pedido |
| `tb_ai_sessao` | `secao_id` | `VARCHAR(20) NULL` | Escopo do ajuste |
| `tb_ai_proposta` | `patch_json` | `JSONB NULL` | Operações propostas (com texto antes/depois para o diff) |
| `tb_ai_proposta` | `operacoes_aceitas` | `JSONB NULL` | Ids aceitos no aplicar; base da métrica de aceite parcial |

`tb_ai_job.tipo` já prevê `AJUSTAR`. `tb_ai_sessao.pagina_id` e `tb_ai_proposta.tipo=ATUALIZACAO`
já existem.

---

## 8. Back-end — peças

| Peça | Responsabilidade |
|---|---|
| `AiPaginaEsboco` (novo) | Parse do HTML para seções e unidades, com ids posicionais e marcação 🔒. É função pura e testada isoladamente |
| `AiPagePatchService` (novo) | Schema do patch, validação, aplicação ordenada e avisos |
| `AiAjustePaginaService` (novo) | Endpoint `/paginas/{id}/ajustes`: valida permissão, status e versão, cria a sessão e enfileira o job |
| `AiJobWorkerService` | Nova etapa para `AJUSTAR`: `prepararEsboco` → `gerarPatch` → `aplicarPatch` → `avaliarQualidade` → `concluir`. Reaproveita progresso, SSE, cancelamento e recuperação |
| `DocFlowAiBridge` | `buscarPaginaParaAjuste(id)` → HTML, versão, status e módulo. Continua sendo a única ponte com o DocFlow |
| `prompts/ajustar-pagina.{system,user}.md` | Prompt versionado (`ajustar-pagina@1.1`), com o esboço entre `<<<ESBOCO … ESBOCO>>>` |
| `AiPropostaService.aplicar` | Ramo ATUALIZAR: confere a versão, aplica `operacoesAceitas` e grava a métrica |

O `FakeLlmProvider` ganha a resposta `TAREFA=AJUSTAR_PAGINA`: altera a primeira unidade editável
com o texto da instrução. É determinístico, para dev e testes.

---

## 9. Limites

| Limite | Valor inicial | Motivo |
|---|---|---|
| Unidades no esboço | 400 | Custo e latência; acima disso, exigir `secaoId` |
| Caracteres no esboço | 30.000 | Idem |
| Operações por proposta | 25 | Diff legível; o autor revisa uma a uma |
| `INSERIR_BLOCO` por proposta | 3 | Ajuste é pontual; muita inserção sugere usar a Fase A |

Os valores entram como constantes nomeadas no `AiPagePatchService`, não em `AiProperties`.
Promover para configuração só se houver necessidade real.

---

## 10. Front-end

| Peça | Responsabilidade |
|---|---|
| Botão "Ajustar com IA" (`pagina-editor-toolbar`) | Visível com `PAGINA:EDITAR` e a feature de IA ligada. Desabilitado com alterações pendentes fora de rascunho (tooltip explica) |
| `app-ai-ajuste-painel` (novo, drawer lateral) | Instrução + escopo (página ou seção, usando `extrairSecoesPagina`) → acompanha a geração reaproveitando `AiGeracaoAcompanhamento` |
| Lista de operações | Checkbox por operação, texto antes/depois com `diffPalavras`, motivo, avisos. Remoções em destaque |
| Prévia | Diff lado a lado do HTML renderizado com as operações marcadas (modo `lado-a-lado` de `pagina-revisoes`) |
| Ações | Aplicar selecionadas · Refinar (instrução) · Rejeitar (motivo) |
| Aplicar | Substitui o conteúdo do editor pelo HTML devolvido e marca a página como alterada. O salvamento é o normal |
| 409 de versão | Mensagem: "A página mudou desde a proposta", com botão "Gerar de novo" |

O `AiGeracaoAcompanhamento`, extraído do wizard, já atende o painel sem mudanças. Foi a razão de
extraí-lo com escopo de componente.

---

## 11. Métricas (alimentam o painel previsto para ADMIN)

- Aceite **por operação**: `operacoes_aceitas / operacoes propostas`, por `prompt_versao` e por
  tipo de operação.
- Rejeições com motivo (já existem) e propostas com avisos (já existem).
- Taxa de 409 por versão: alta indica que o autor edita enquanto espera. Nesse caso, avaliar
  travar o editor durante a geração.

---

## 12. Testes

| Nível | Casos principais |
|---|---|
| `AiPaginaEsbocoTest` | Agrupamento igual ao do front (fixture HTML compartilhada); ids estáveis; 🔒 para inline; imagens e placeholders ignorados |
| `AiPagePatchServiceTest` | Aplicação ordenada; id inexistente e 🔒 viram aviso; componente fora do catálogo rejeitado; subconjunto de operações; HTML sanitizado; imagens preservadas |
| `AiAjustePaginaServiceTest` | 403 sem EDITAR; 422 arquivada; 409 versão; 422 esboço grande sem seção |
| `AiPropostaServiceTest` | `aplicar` ATUALIZAR: 409 quando a versão mudou; grava `operacoes_aceitas` |
| Worker | Job `AJUSTAR` com fake: proposta ATUALIZACAO com patch e HTML resultante |
| Front | Painel: seleção parcial envia só os ids marcados; 409 mostra "Gerar de novo"; editor fica alterado após aplicar |
| Fixture compartilhada | `docs/doc-flow/fixtures/esboco-pagina.html` usada pelo teste Java do esboço e pelo spec de `extrairSecoesPagina`, para garantir o mesmo agrupamento nos dois lados |

---

## 13. Plano de entrega (substitui AI-501..507)

| Ticket | Tipo | Entrega | Pronto quando |
|---|---|---|---|
| AI-501 | API | `AiPaginaEsboco` + fixture compartilhada | Testes de agrupamento e 🔒 verdes |
| AI-502 | API | `AiPagePatchService` (schema, aplicar, avisos) | Testes de aplicação verdes |
| AI-503 | DB+API | V41 + endpoint `/paginas/{id}/ajustes` + job `AJUSTAR` no worker + prompt | Fake gera proposta ATUALIZACAO ponta a ponta |
| AI-504 | API | `aplicar` com `operacoesAceitas` + 409 de versão + métrica | Testes de concorrência verdes |
| AI-505 | WEB | Botão na toolbar + painel com instrução, escopo e acompanhamento | Proposta aparece no painel |
| AI-506 | WEB | Lista de operações, diff e aplicar selecionadas | Editor alterado com o subconjunto; salvar gera revisão |
| AI-507 | DOC+OPS | Atualizar docs 10/11, `docs/ai/README` "Onde mexer" e smoke manual | Roteiro do §15 executado |

Ordem: 501 → 502 → 503 → 504 (API testável com fake) → 505 → 506 → 507.
Estimativa: ~1 sprint, com o back-end na primeira metade.

---

## 14. Decisões

### Tomadas neste desenho

1. **Patch sobre o HTML atual, não regeneração da PageSpec** (§2).
2. **O modelo não escreve HTML**: só altera textos, insere componentes do catálogo e remove unidades.
3. **Aplicar só no editor (FORM)**: o autor salva, e a revisão e o workflow seguem iguais.
4. **Endpoint próprio por página**, sem triagem: a instrução já é o escopo.
5. **Versão base obrigatória** e 409 ao divergir.

### Decididas com o time (2026-10-03)

| | Decisão |
|---|---|
| D1 | **Sem PERSISTIR** na Fase B: o ajuste só vai para o editor |
| D2 | **Unidades com formatação inline ficam somente leitura** na v1. Medir pelos avisos de "descartada" |
| D3 | **Regra geral corrigida**: `PaginaService.atualizar` recusa (422) alterar título, slug, código, resumo ou conteúdo de página `APROVADO`/`PUBLICADO`. O editor mostra "Conteúdo travado" com "Voltar para rascunho", avisando que a página sai das próximas publicações. Metadados (ordem, módulo, pai, ativo) continuam livres. O endpoint de ajuste recusa as mesmas páginas |
| D4 | **Remoção permitida**, sempre desmarcada por padrão no painel |

### Como foi discutido (histórico)

- **D1. PERSISTIR direto?** Permitir "aplicar e salvar" sem abrir o editor (útil para ajustes em
  lote no futuro). Recomendação: **não na Fase B**; reavaliar com métricas de aceite.
- **D2. Unidades com formatação inline** (links, negrito, código). Na v1 elas ficam 🔒.
  Alternativa v2: enviar com um subconjunto de Markdown inline (`**`, `` ` ``, `[texto](#lN)`, com o
  link referenciado por id) e reconstruir no servidor. Recomendação: **v1 travada** e medir quantos
  pedidos esbarram nisso pelos avisos.
- **D3. Páginas fora de rascunho.** Verificado no código: `PaginaService.atualizar` não checa o
  status e `Pagina.atualizar` não o altera. Salvar uma página `APROVADO` ou `PUBLICADO` mantém o
  status, ou seja, o conteúdo aprovado muda sem nova revisão. Isso vale para qualquer edição
  manual, não só para a IA, mas a Fase B facilitaria o caminho. Opções:
  (a) a IA segue a regra atual;
  (b) aplicar um ajuste em página fora de rascunho a devolve para `RASCUNHO`;
  (c) corrigir a regra geral: salvar conteúdo de página aprovada ou publicada volta para rascunho
  ou exige reenvio para revisão.
  Recomendação: **(c)**, como correção do fluxo editorial antes da AI-503. A Fase B então só
  herda a regra.
- **D4. Remoção.** Permitir `REMOVER_UNIDADE` na v1 ou só alterar e inserir? Recomendação:
  **permitir**, sempre desmarcada por padrão no painel, para o autor optar.

---

## 14.1 Diferenças entre o desenho e a implementação

- **Painel inline, não drawer.** O `shared/ui` não tem drawer: o painel abre abaixo da linha de
  status do editor, com a página visível embaixo.
- **Ajuste exige a página salva em qualquer status.** A ideia de autosave antes do pedido virou
  uma regra mais simples: com alterações pendentes, o botão avisa "Salve a página antes".
- **Prévia** mostra a página com todas as mudanças propostas. O diff por operação é palavra a
  palavra na lista; o lado a lado com só as selecionadas ficou para depois.
- **Fixture compartilhada** front/back não foi criada como arquivo: o mesmo HTML está no
  `AiPaginaEsbocoTest` (Java). O agrupamento do front continua coberto pelos specs do
  `pagina-section-organizer`. Se as duas regras divergirem, as seções do escopo mudam de id.

## 15. Smoke manual (critério de aceite da Fase B)

1. Página em rascunho → "Ajustar com IA" → "reescreva os pré-requisitos para gestor", escopo
   "Pré-requisitos" → proposta com operações só nessa seção.
2. Desmarcar uma operação → aplicar → editor mostra só as aceitas → salvar → nova revisão no
   histórico com diff coerente.
3. Pedir ajuste, editar a página em outra aba e aplicar → 409 com "Gerar de novo".
4. Página com links nos itens → as unidades com link aparecem como não editáveis; o aviso
   aparece se a instrução pedia para mudá-las.
5. Página com imagens anexadas → imagens intactas após aplicar.
