# Prompt ops: ajustar prompts pelos padrões de rejeição

Ciclo para melhorar a geração sem chute: medir, mudar uma coisa, comparar. Tudo o que é preciso já
é gravado em cada proposta (`tb_ai_proposta`): a versão do prompt (`prompt_versao`), o status
(aceita / rejeitada / regenerada), a categoria e o motivo da rejeição, e a página criada.

## 1. Ler o painel

**Qualidade da IA** (`/doc-flow/ia-qualidade`, permissão `AUDITORIA:VISUALIZAR`), período de 30
ou 90 dias:

| Bloco | Pergunta que responde |
|---|---|
| Versão atual × anterior | A última mudança de prompt melhorou ou piorou? |
| Por versão de prompt | Aceite, regenerações e texto mantido de cada versão |
| Por que rejeitaram | Qual categoria domina as rejeições |
| Depois do aceite | O que os autores corrigem mesmo quando aceitam |
| Rejeições recentes | Os motivos em texto livre, com a versão que os gerou |
| Avisos mais frequentes | Gerações que caíram em fallback (resposta inválida, timeout) |

Regras de leitura:

- **Aceite** = aceitas ÷ (aceitas + rejeitadas + regeneradas). Pendentes não contam.
- **Texto mantido** = quanto do texto da IA continua na página hoje. Aceite alto com texto
  mantido baixo indica que o autor aceita só para reescrever.
- Com menos de **10 propostas decididas**, a versão aparece com o selo *amostra pequena*. A
  diferença ainda pode ser ruído.

## 2. Do padrão para a mudança

Escolha **um** padrão por ciclo, o mais frequente, e mude só o que o ataca.

| Padrão no painel | Onde mexer |
|---|---|
| *Conteúdo incorreto ou inventado* | `gerar-page-spec.system.md`: regras de "não inventar" (campos, regras de negócio, nomes de tela). Reforce usar só o briefing. |
| *Faltou informação* | Triagem (`AiTriagemService`: perguntas obrigatórias) ou `gerar-page-spec.user.md`, para pedir explicitamente o que costuma faltar (filtros, permissões, mensagens). |
| *Estrutura ou seções inadequadas* | Composição por tipo de página (`docflow/.../pagina-blueprints.json`) antes do prompt. |
| *Modelo ou componentes errados* | Recomendação de modelo (`AiTemplateRecomendacaoService`) e descrições dos blocos (`pagina-blocos.json`). |
| *Tom ou linguagem* | Seção de estilo do `gerar-page-spec.system.md`. |
| Depois do aceite: *título/código alterado* | Instruções de `tituloSugerido` / `codigoTelaSugerido` no `user.md`. |
| Ajustes (Fase B) com tipo de mudança pouco aceito | `ajustar-pagina.*.md` e os limites em `AiPagePatchService`. |
| Avisos de fallback frequentes | Formato de saída (JSON Schema) e tamanho do contexto, não o tom. |

## 3. Mudar o prompt

1. Edite o `.md` em `ai/src/main/resources/prompts/` e **suba `versao`** no cabeçalho.
2. Rode `./mvnw -pl ai test`. O `AiPromptVersaoTest` falha e imprime a linha nova para
   `ai/src/test/resources/prompts-versoes.lock`. Cole a linha e rode de novo.
   - Texto mudou sem subir a versão? O teste falha: duas propostas com o mesmo
     `prompt_versao` nunca podem vir de textos diferentes.
3. Faça um smoke local com a IA real (`docs/ai/RUNBOOK-LOCAL.md`): 2 ou 3 briefings que
   exercitem o padrão escolhido.
4. No commit, cite o padrão e a versão. Exemplo: `prompt(ai): gerar-page-spec@2.3 pede filtros
   explicitamente (Faltou informação 41%)`.

## 4. Avaliar e decidir

Depois do deploy, espere ao menos 10 propostas decididas da versão nova (de preferência 20 ou
mais) e compare no bloco **Versão atual × anterior**:

- **Aceite subiu e a categoria atacada caiu:** mantenha e comece o próximo ciclo.
- **Aceite igual:** a mudança não pegou o problema. Volte ao passo 2 com outra hipótese.
- **Aceite caiu ou o texto mantido caiu:** reverta. Volte o texto anterior **subindo a
  versão de novo** (ex.: 2.3 → 2.4 com o texto da 2.2). Assim o painel separa os períodos.

Evite mudar dois prompts da mesma família no mesmo deploy, ou não dá para saber qual causou o
efeito.

## Onde fica cada coisa

| O quê | Onde |
|---|---|
| Regra do aceite, categorias, "depois do aceite" | `AiMetricasService` |
| Categorias de rejeição | `AiCategoriaRejeicao` (back) e `CATEGORIAS_REJEICAO` (front) |
| Comparação entre versões no painel | `utils/ai-prompt-comparacao.util.ts` (front) |
| Trava de versão dos prompts | `AiPromptVersaoTest` + `prompts-versoes.lock` |
