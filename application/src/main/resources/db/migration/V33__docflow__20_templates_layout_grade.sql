-- Expõe opções de layout da grade nos templates: tela toda (--1), meia tela (--2) e 3 colunas (--3).

UPDATE tb_pagina_template SET
  descricao = 'Explica filtros, listagem e ações. Use annotation-grid--1 (tela toda), --2 (meia tela) ou --3 (3 colunas).',
  conteudo_html = $html$
<section class="doc-intro"><span class="doc-kicker">Consulta e listagem</span><h2>Consulta de [registros]</h2><p>Apresente as informações disponíveis e as situações em que esta consulta é útil.</p></section>
<div class="objective-card"><p><strong>Objetivo desta tela</strong></p><p>Explique quais decisões ou tarefas a consulta ajuda o usuário a realizar.</p></div>
<section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura da consulta</strong><span>Deixe filtros, tabela e ações visíveis.</span></div><figcaption>Visão geral dos filtros e resultados.</figcaption></figure></section>
<div class="callout"><strong>Layout da grade</strong><p>Troque a classe da grade conforme o espaço desejado: <code>annotation-grid--1</code> (tela toda), <code>annotation-grid--2</code> (meia tela) ou <code>annotation-grid--3</code> (3 colunas). O mesmo vale para <code>content-grid--1</code>, <code>--2</code> e <code>--3</code>.</p></div>
<section class="doc-section"><h2>Elementos da tela</h2><div class="annotation-grid annotation-grid--3"><article class="annotation-card"><h3>Filtros</h3><p>Descreva os principais critérios de pesquisa.</p></article><article class="annotation-card"><h3>Pesquisar e limpar</h3><p>Explique o comportamento de cada ação.</p></article><article class="annotation-card"><h3>Resultados</h3><p>Apresente as colunas e a ordenação padrão.</p></article><article class="annotation-card"><h3>Ações por registro</h3><p>Detalhe visualizar, editar e excluir.</p></article></div></section>
<section class="doc-section"><div class="content-grid content-grid--2"><article class="rule-card"><h2>Regras de negócio</h2><ul><li>Informe limites e combinações de filtros.</li><li>Explique visibilidade e permissões.</li></ul></article><article class="rule-card"><h2>Exemplos de uso</h2><ul><li>Localizar registros por nome ou período.</li><li>Filtrar itens por situação.</li><li>Abrir os detalhes de um resultado.</li></ul></article></div></section>
$html$,
  updated_at = now(),
  updated_by = 'migration'
WHERE codigo = 'CONSULTA';

UPDATE tb_pagina_template SET
  descricao = 'Documenta cadastro com captura e marcações. Layout da grade: content-grid/annotation-grid --1, --2 ou --3.',
  conteudo_html = $html$
<section class="doc-intro"><span class="doc-kicker">Cadastro e edição</span><h2>Cadastro de [entidade]</h2><p>Explique quando este cadastro deve ser usado e o que será registrado.</p></section>
<div class="objective-card"><p><strong>Objetivo desta tela</strong></p><p>Descreva o valor do cadastro para a operação e quem pode realizá-lo.</p></div>
<section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura da tela de cadastro</strong><span>Prefira uma imagem limpa, com os campos principais visíveis.</span></div><figcaption>Visão geral da tela de cadastro.</figcaption></figure></section>
<div class="callout"><strong>Layout da grade</strong><p>Use <code>annotation-grid--1</code> para ocupar a tela toda, <code>--2</code> para meia tela ou <code>--3</code> para três colunas.</p></div>
<section class="doc-section"><h2>Elementos da tela</h2><div class="annotation-grid annotation-grid--2"><article class="annotation-card"><h3>Campo principal</h3><p>Informe formato, limite e exemplo de preenchimento.</p></article><article class="annotation-card"><h3>Campos complementares</h3><p>Explique quando cada informação deve ser preenchida.</p></article><article class="annotation-card"><h3>Status ou opção</h3><p>Descreva o efeito de cada valor disponível.</p></article><article class="annotation-card"><h3>Ações</h3><p>Detalhe os botões Salvar, Cancelar e outras ações.</p></article></div></section>
<section class="doc-section"><div class="content-grid content-grid--2"><article class="rule-card"><h2>Regras de negócio</h2><ul class="checklist"><li>Registre uma validação obrigatória.</li><li>Explique duplicidade, limites ou dependências.</li></ul></article><article class="rule-card"><h2>Como cadastrar</h2><ol><li>Acesse a tela.</li><li>Preencha os campos indicados.</li><li>Revise e selecione <strong>Salvar</strong>.</li></ol></article></div></section>
<div class="callout"><strong>Artigos relacionados</strong><p>Inclua links para consulta, edição ou processos ligados a este cadastro.</p></div>
$html$,
  updated_at = now(),
  updated_by = 'migration'
WHERE codigo = 'CADASTRO';

UPDATE tb_pagina_template SET
  descricao = 'Apresenta objetivo, visão da tela e pontos. Grades com --1 (tela toda), --2 (meia) ou --3 (3 colunas).',
  conteudo_html = $html$
<section class="doc-intro"><span class="doc-kicker">Guia da funcionalidade</span><h2>Visão geral da funcionalidade</h2><p>Apresente em uma frase o que o recurso permite fazer e para quem ele foi criado.</p></section>
<div class="objective-card"><p><strong>Objetivo desta funcionalidade</strong></p><p>Explique o ganho para o usuário e o resultado de negócio esperado.</p></div>
<section class="doc-section"><h2>Visão da tela</h2><p>Use esta área para orientar visualmente os principais pontos da interface.</p><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira aqui uma captura da tela</strong><span>Use a ferramenta de imagem do editor e substitua este bloco.</span></div><figcaption>Tela principal da funcionalidade.</figcaption></figure></section>
<div class="callout"><strong>Layout da grade</strong><p>Altere a classe: <code>annotation-grid--1</code> tela toda · <code>annotation-grid--2</code> meia tela · <code>annotation-grid--3</code> três colunas.</p></div>
<section class="doc-section"><h2>Principais elementos</h2><div class="annotation-grid annotation-grid--3"><article class="annotation-card"><h3>Acesso principal</h3><p>Indique onde o usuário inicia a operação.</p></article><article class="annotation-card"><h3>Área de trabalho</h3><p>Explique onde os dados são exibidos ou preenchidos.</p></article><article class="annotation-card"><h3>Ação de confirmação</h3><p>Mostre como concluir e salvar a operação.</p></article></div></section>
<section class="doc-section"><h2>Regras e orientações</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Regra de negócio</h3><p>Descreva uma validação importante e seu impacto.</p></article><article class="rule-card"><h3>Boa prática</h3><p>Registre uma recomendação que evite erros recorrentes.</p></article></div></section>
<div class="result-card"><strong>Resultado esperado</strong><p>Informe o que será apresentado após a conclusão bem-sucedida.</p></div>
$html$,
  updated_at = now(),
  updated_by = 'migration'
WHERE codigo = 'FUNCIONALIDADE';
