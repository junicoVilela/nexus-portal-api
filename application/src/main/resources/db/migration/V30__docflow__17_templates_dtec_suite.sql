-- Modelos de sistema derivados das jornadas principais do DTEC Suite.
INSERT INTO tb_pagina_template
  (id, codigo, nome, descricao, conteudo_html, ordem, ativo, created_by, updated_by)
VALUES
  ('10000000-0000-0000-0000-000000000013', 'DTEC_ALERTAS', 'DTEC · Pesquisa de alertas',
   'Documenta filtros, fila de análise, resultado de alertas e ações disponíveis.', $html$
<section class="doc-intro"><span class="doc-kicker">DTEC Suite · Alertas</span><h2>Pesquisar alertas</h2><p>Explique quando consultar alertas e qual decisão esta pesquisa apoia.</p></section>
<div class="objective-card"><p><strong>Objetivo</strong></p><p>Localizar apontamentos a partir de critérios de busca e registrar o tratamento adequado.</p></div>
<section class="doc-section"><h2>Antes de começar</h2><ul class="checklist"><li>Confirme que possui acesso à fila correta.</li><li>Defina os dados que serão usados como critério.</li><li>Evite incluir dados sensíveis em evidências compartilhadas.</li></ul></section>
<section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura da pesquisa de alertas</strong><span>Mostre filtros, fila e lista de resultados.</span></div><figcaption>Pesquisa de alertas no DTEC Suite.</figcaption></figure></section>
<section class="doc-section"><h2>Como pesquisar</h2><ol class="flow-strip"><li><strong>Selecionar fila</strong><span>Escolha ou crie o contexto de análise.</span></li><li><strong>Adicionar filtros</strong><span>Defina os critérios de busca.</span></li><li><strong>Filtrar</strong><span>Execute a consulta.</span></li><li><strong>Analisar</strong><span>Abra o alerta ou cliente encontrado.</span></li></ol></section>
<section class="doc-section"><h2>Elementos importantes</h2><div class="annotation-grid"><article class="annotation-card"><h3>Fila</h3><p>Explique o contexto, grupo de acesso e responsáveis.</p></article><article class="annotation-card"><h3>Critérios</h3><p>Descreva como adicionar, editar e remover filtros.</p></article><article class="annotation-card"><h3>Resultados</h3><p>Explique ordenação, apontamentos e navegação para o detalhe.</p></article></div></section>
<div class="warning"><strong>Atenção</strong><p>Registre quais filtros são obrigatórios e quando uma pesquisa deve ser salva para reutilização.</p></div>
$html$, 90, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000014', 'DTEC_SIMULACAO', 'DTEC · Simulação de processamento',
   'Guia para criar, acompanhar e interpretar uma simulação de processamento.', $html$
<section class="doc-intro"><span class="doc-kicker">DTEC Suite · Simulação</span><h2>Executar uma simulação de processamento</h2><p>Apresente quando uma simulação deve ser solicitada e o resultado esperado.</p></section>
<section class="doc-section"><h2>Visão do painel</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura do painel de simulações</strong><span>Mostre o lote, status, métricas e regras selecionadas.</span></div><figcaption>Painel de simulações.</figcaption></figure></section>
<section class="doc-section"><h2>Fluxo da simulação</h2><ol class="flow-strip"><li><strong>Nova simulação</strong><span>Solicite uma execução.</span></li><li><strong>Configurar</strong><span>Defina regras, ponto de corte e filtros.</span></li><li><strong>Acompanhar</strong><span>Verifique o status da fila.</span></li><li><strong>Analisar</strong><span>Consulte clientes e alertas resultantes.</span></li></ol></section>
<section class="doc-section"><h2>Leitura dos resultados</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Métricas</h3><p>Explique lote, clientes analisados, clientes apontados, regras e tempo de processamento.</p></article><article class="rule-card"><h3>Status</h3><p>Descreva os estados pendente, em processamento, concluída e falha.</p></article></div></section>
<div class="result-card"><strong>Validação final</strong><p>Confirme que a simulação terminou e que os resultados correspondem aos filtros e regras escolhidos.</p></div>
$html$, 91, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000015', 'DTEC_ANALISE_CLIENTE', 'DTEC · Análise de cliente',
   'Documenta a consulta de dados do cliente, parecer e retorno à análise.', $html$
<section class="doc-intro"><span class="doc-kicker">DTEC Suite · Análise</span><h2>Analisar um cliente</h2><p>Explique como interpretar os dados apresentados e registrar um parecer.</p></section>
<section class="doc-section"><h2>Visão da análise</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura do detalhe do cliente</strong><span>Destaque identificação, dados dinâmicos e ação de parecer.</span></div><figcaption>Detalhe de cliente na análise.</figcaption></figure></section>
<section class="doc-section"><h2>Procedimento</h2><ol><li>Abra o cliente a partir de um alerta ou resultado de simulação.</li><li>Confira nome, documento e os dados dinâmicos disponíveis.</li><li>Compare as informações com os critérios que originaram o apontamento.</li><li>Registre o parecer quando tiver permissão.</li><li>Retorne à lista para continuar a análise.</li></ol></section>
<section class="doc-section"><h2>Critérios de decisão</h2><div class="content-grid content-grid--3"><article class="rule-card"><h3>Evidências</h3><p>Liste dados que sustentam a decisão.</p></article><article class="rule-card"><h3>Parecer</h3><p>Explique o formato, responsabilidade e impacto do registro.</p></article><article class="rule-card"><h3>Sigilo</h3><p>Oriente o tratamento de documentos e dados pessoais.</p></article></div></section>
$html$, 92, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000016', 'DTEC_REGRAS', 'DTEC · Regras de processamento',
   'Modelo para regras: objetivo, condições, parâmetros, versão, validação e impacto.', $html$
<section class="doc-intro"><span class="doc-kicker">DTEC Suite · Parâmetros</span><h2>Configurar uma regra de processamento</h2><p>Descreva a finalidade da regra e quais resultados ela influencia.</p></section>
<div class="objective-card"><p><strong>Antes de alterar uma regra</strong></p><p>Confirme a autorização, a versão vigente e os impactos esperados em alertas e simulações.</p></div>
<section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura da lista ou edição de regras</strong><span>Mostre nome, versão, condições e ações de salvar.</span></div><figcaption>Configuração de regras.</figcaption></figure></section>
<section class="doc-section"><h2>Como configurar</h2><ol class="flow-strip"><li><strong>Localizar</strong><span>Pesquise uma regra existente ou inicie uma nova.</span></li><li><strong>Definir condições</strong><span>Informe critérios e parâmetros.</span></li><li><strong>Revisar impacto</strong><span>Valide pontos e comportamento esperado.</span></li><li><strong>Salvar</strong><span>Registre a versão para uso futuro.</span></li></ol></section>
<section class="doc-section"><h2>Regras de negócio</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Versionamento</h3><p>Explique quando uma alteração cria nova versão e como recuperar a anterior.</p></article><article class="rule-card"><h3>Validação</h3><p>Registre testes obrigatórios antes de usar a regra em uma simulação.</p></article></div></section>
$html$, 93, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000017', 'DTEC_PARAMETROS', 'DTEC · Cadastro de parâmetros',
   'Guia para cadastros corporativos, dados auxiliares e provisionamento de saque.', $html$
<section class="doc-intro"><span class="doc-kicker">DTEC Suite · Parâmetros</span><h2>Manter parâmetros do sistema</h2><p>Explique qual dado será cadastrado, onde ele é utilizado e quem pode mantê-lo.</p></section>
<section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura do cadastro de parâmetro</strong><span>Mostre campos obrigatórios, ações e a lista de registros.</span></div><figcaption>Cadastro de parâmetros no DTEC Suite.</figcaption></figure></section>
<section class="doc-section"><h2>Como incluir ou editar</h2><ol><li>Acesse o grupo de parâmetros correspondente.</li><li>Selecione <strong>Novo</strong> ou abra o registro existente.</li><li>Preencha os campos obrigatórios conforme a regra de negócio.</li><li>Revise os dados e salve.</li><li>Confirme o registro na lista.</li></ol></section>
<section class="doc-section"><h2>Campos e validações</h2><div class="annotation-grid"><article class="annotation-card"><h3>Identificação</h3><p>Descreva código, nome e unicidade.</p></article><article class="annotation-card"><h3>Vigência</h3><p>Explique datas, status e condições de ativação.</p></article><article class="annotation-card"><h3>Exclusão</h3><p>Informe dependências e quando a remoção é irreversível.</p></article></div></section>
<div class="warning"><strong>Impacto operacional</strong><p>Registre quais processamentos, alertas ou relatórios podem ser afetados por esta alteração.</p></div>
$html$, 94, TRUE, 'migration', 'migration')
ON CONFLICT (codigo) DO UPDATE SET
  nome = EXCLUDED.nome,
  descricao = EXCLUDED.descricao,
  conteudo_html = EXCLUDED.conteudo_html,
  ordem = EXCLUDED.ordem,
  ativo = TRUE,
  updated_at = now(),
  updated_by = 'migration';
