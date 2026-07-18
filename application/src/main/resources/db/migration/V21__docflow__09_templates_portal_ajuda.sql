-- Evolui o catálogo de templates para o sistema visual do portal de ajuda.
-- Páginas já criadas não são alteradas: somente novos usos do catálogo recebem estes blocos.

INSERT INTO tb_pagina_template
  (id, codigo, nome, descricao, conteudo_html, ordem, ativo, created_by, updated_by)
VALUES
  ('10000000-0000-0000-0000-000000000001', 'FUNCIONALIDADE', 'Guia de funcionalidade',
   'Apresenta objetivo, visão da tela, pontos numerados, regras e resultado esperado.', $html$
<section class="doc-intro"><span class="doc-kicker">Guia da funcionalidade</span><h2>Visão geral da funcionalidade</h2><p>Apresente em uma frase o que o recurso permite fazer e para quem ele foi criado.</p></section>
<div class="objective-card"><p><strong>Objetivo desta funcionalidade</strong></p><p>Explique o ganho para o usuário e o resultado de negócio esperado.</p></div>
<section class="doc-section"><h2>Visão da tela</h2><p>Use esta área para orientar visualmente os principais pontos da interface.</p><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira aqui uma captura da tela</strong><span>Use a ferramenta de imagem do editor e substitua este bloco.</span></div><figcaption>Tela principal da funcionalidade.</figcaption></figure></section>
<section class="doc-section"><h2>Principais elementos</h2><div class="annotation-grid"><article class="annotation-card"><h3>Acesso principal</h3><p>Indique onde o usuário inicia a operação.</p></article><article class="annotation-card"><h3>Área de trabalho</h3><p>Explique onde os dados são exibidos ou preenchidos.</p></article><article class="annotation-card"><h3>Ação de confirmação</h3><p>Mostre como concluir e salvar a operação.</p></article></div></section>
<section class="doc-section"><h2>Regras e orientações</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Regra de negócio</h3><p>Descreva uma validação importante e seu impacto.</p></article><article class="rule-card"><h3>Boa prática</h3><p>Registre uma recomendação que evite erros recorrentes.</p></article></div></section>
<div class="result-card"><strong>Resultado esperado</strong><p>Informe o que será apresentado após a conclusão bem-sucedida.</p></div>
$html$, 10, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000002', 'PASSO_A_PASSO', 'Passo a passo visual',
   'Guia operacional com preparação, fluxo horizontal, etapas e validação final.', $html$
<section class="doc-intro"><span class="doc-kicker">Guia operacional</span><h2>Como realizar o procedimento</h2><p>Resuma o procedimento, quando ele deve ser executado e o tempo estimado.</p></section>
<div class="objective-card"><p><strong>Antes de começar</strong></p><p>Liste permissões, dados e condições necessárias para seguir este guia.</p></div>
<section class="doc-section"><h2>Fluxo do procedimento</h2><ol class="flow-strip"><li><strong>Acessar</strong><span>Abra a tela indicada.</span></li><li><strong>Preencher</strong><span>Informe os dados solicitados.</span></li><li><strong>Revisar</strong><span>Confira as informações.</span></li><li><strong>Concluir</strong><span>Confirme a operação.</span></li></ol></section>
<section class="doc-section"><h2>Passo a passo detalhado</h2><div class="screen-grid"><article class="guide-card"><span class="guide-card__number">1</span><div class="screen-placeholder screen-placeholder--compact"><strong>Captura da etapa 1</strong><span>Insira a imagem correspondente.</span></div><h3>Acesse a funcionalidade</h3><p>Indique o caminho de navegação e o ponto de entrada.</p></article><article class="guide-card"><span class="guide-card__number">2</span><div class="screen-placeholder screen-placeholder--compact"><strong>Captura da etapa 2</strong><span>Insira a imagem correspondente.</span></div><h3>Preencha e revise</h3><p>Explique os dados necessários e as validações da tela.</p></article><article class="guide-card"><span class="guide-card__number">3</span><div class="screen-placeholder screen-placeholder--compact"><strong>Captura da etapa 3</strong><span>Insira a imagem correspondente.</span></div><h3>Confirme a operação</h3><p>Mostre a ação final e a mensagem apresentada.</p></article></div></section>
<div class="result-card"><strong>Como validar</strong><p>Explique como confirmar visualmente que o procedimento funcionou.</p></div>
$html$, 20, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000003', 'CADASTRO', 'Cadastro ou edição',
   'Documenta uma tela de cadastro com captura, marcações, regras e passos.', $html$
<section class="doc-intro"><span class="doc-kicker">Cadastro e edição</span><h2>Cadastro de [entidade]</h2><p>Explique quando este cadastro deve ser usado e o que será registrado.</p></section>
<div class="objective-card"><p><strong>Objetivo desta tela</strong></p><p>Descreva o valor do cadastro para a operação e quem pode realizá-lo.</p></div>
<section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura da tela de cadastro</strong><span>Prefira uma imagem limpa, com os campos principais visíveis.</span></div><figcaption>Visão geral da tela de cadastro.</figcaption></figure></section>
<section class="doc-section"><h2>Elementos da tela</h2><div class="annotation-grid"><article class="annotation-card"><h3>Campo principal</h3><p>Informe formato, limite e exemplo de preenchimento.</p></article><article class="annotation-card"><h3>Campos complementares</h3><p>Explique quando cada informação deve ser preenchida.</p></article><article class="annotation-card"><h3>Status ou opção</h3><p>Descreva o efeito de cada valor disponível.</p></article><article class="annotation-card"><h3>Ações</h3><p>Detalhe os botões Salvar, Cancelar e outras ações.</p></article></div></section>
<section class="doc-section"><div class="content-grid content-grid--2"><article class="rule-card"><h2>Regras de negócio</h2><ul class="checklist"><li>Registre uma validação obrigatória.</li><li>Explique duplicidade, limites ou dependências.</li></ul></article><article class="rule-card"><h2>Como cadastrar</h2><ol><li>Acesse a tela.</li><li>Preencha os campos indicados.</li><li>Revise e selecione <strong>Salvar</strong>.</li></ol></article></div></section>
<div class="callout"><strong>Artigos relacionados</strong><p>Inclua links para consulta, edição ou processos ligados a este cadastro.</p></div>
$html$, 30, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000004', 'CONSULTA', 'Consulta ou listagem',
   'Explica filtros, listagem, ações disponíveis, regras e exemplos de uso.', $html$
<section class="doc-intro"><span class="doc-kicker">Consulta e listagem</span><h2>Consulta de [registros]</h2><p>Apresente as informações disponíveis e as situações em que esta consulta é útil.</p></section>
<div class="objective-card"><p><strong>Objetivo desta tela</strong></p><p>Explique quais decisões ou tarefas a consulta ajuda o usuário a realizar.</p></div>
<section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura da consulta</strong><span>Deixe filtros, tabela e ações visíveis.</span></div><figcaption>Visão geral dos filtros e resultados.</figcaption></figure></section>
<section class="doc-section"><h2>Elementos da tela</h2><div class="annotation-grid"><article class="annotation-card"><h3>Filtros</h3><p>Descreva os principais critérios de pesquisa.</p></article><article class="annotation-card"><h3>Pesquisar e limpar</h3><p>Explique o comportamento de cada ação.</p></article><article class="annotation-card"><h3>Resultados</h3><p>Apresente as colunas e a ordenação padrão.</p></article><article class="annotation-card"><h3>Ações por registro</h3><p>Detalhe visualizar, editar e excluir.</p></article></div></section>
<section class="doc-section"><div class="content-grid content-grid--2"><article class="rule-card"><h2>Regras de negócio</h2><ul><li>Informe limites e combinações de filtros.</li><li>Explique visibilidade e permissões.</li></ul></article><article class="rule-card"><h2>Exemplos de uso</h2><ul><li>Localizar registros por nome ou período.</li><li>Filtrar itens por situação.</li><li>Abrir os detalhes de um resultado.</li></ul></article></div></section>
$html$, 40, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000007', 'DICIONARIO_CAMPOS', 'Dicionário de campos',
   'Catálogo visual de campos com obrigatoriedade, exemplo e impacto no negócio.', $html$
<section class="doc-intro"><span class="doc-kicker">Referência de dados</span><h2>Dicionário de campos — [tela]</h2><p>Documente o significado de cada campo e o impacto de um preenchimento incorreto.</p></section>
<div class="objective-card"><p><strong>Objetivo deste dicionário</strong></p><p>Padronizar o entendimento funcional e apoiar usuários, produto, suporte e desenvolvimento.</p></div>
<section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura com os campos numerados</strong><span>Use os números da imagem na primeira coluna da tabela.</span></div><figcaption>Campos mapeados para o dicionário abaixo.</figcaption></figure></section>
<section class="doc-section"><h2>Descrição dos campos</h2><div class="table-wrap"><table class="dictionary-table"><thead><tr><th>#</th><th>Campo</th><th>Descrição</th><th>Obrigatório?</th><th>Exemplo</th><th>Impacto no negócio</th></tr></thead><tbody><tr><td><span class="number-badge">1</span></td><td><strong>Nome do campo</strong></td><td>Explique finalidade, formato e origem da informação.</td><td><span class="status-badge status-badge--required">Sim</span></td><td>Valor de exemplo</td><td>Descreva o efeito deste dado nos processos.</td></tr><tr><td><span class="number-badge">2</span></td><td><strong>Outro campo</strong></td><td>Detalhe regras, limites e dependências.</td><td><span class="status-badge">Não</span></td><td>Valor de exemplo</td><td>Informe relatórios, integrações ou decisões afetadas.</td></tr></tbody></table></div></section>
<section class="doc-section"><h2>Observações importantes</h2><div class="content-grid content-grid--3"><article class="rule-card"><h3>Validações</h3><p>Liste formatos e mensagens de erro.</p></article><article class="rule-card"><h3>Dependências</h3><p>Explique campos condicionais e relacionamentos.</p></article><article class="rule-card"><h3>Segurança</h3><p>Indique dados sensíveis e restrições de acesso.</p></article></div></section>
$html$, 50, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000008', 'PROCESSO', 'Fluxo de processo',
   'Documenta etapas, telas envolvidas, regras e resumo de um processo ponta a ponta.', $html$
<section class="doc-intro"><span class="doc-kicker">Processo ponta a ponta</span><h2>Processo de [nome do processo]</h2><p>Explique o início, o fim e os participantes deste fluxo.</p></section>
<div class="objective-card"><p><strong>Objetivo do processo</strong></p><p>Descreva o resultado de negócio, as áreas envolvidas e o tempo esperado.</p></div>
<section class="doc-section"><h2>Visão geral do fluxo</h2><ol class="flow-strip"><li><strong>Solicitação</strong><span>O processo é iniciado.</span></li><li><strong>Análise</strong><span>Os dados são validados.</span></li><li><strong>Aprovação</strong><span>O responsável decide.</span></li><li><strong>Conclusão</strong><span>O resultado é registrado.</span></li></ol></section>
<section class="doc-section"><h2>Telas envolvidas</h2><div class="screen-grid"><article class="guide-card"><span class="guide-card__number">1</span><div class="screen-placeholder screen-placeholder--compact"><strong>Tela de entrada</strong><span>Insira uma captura.</span></div><h3>Início do processo</h3><p>Explique como a solicitação é registrada.</p></article><article class="guide-card"><span class="guide-card__number">2</span><div class="screen-placeholder screen-placeholder--compact"><strong>Tela de análise</strong><span>Insira uma captura.</span></div><h3>Análise e decisão</h3><p>Mostre validações, responsáveis e ações.</p></article><article class="guide-card"><span class="guide-card__number">3</span><div class="screen-placeholder screen-placeholder--compact"><strong>Tela final</strong><span>Insira uma captura.</span></div><h3>Conclusão</h3><p>Descreva o retorno e os registros gerados.</p></article></div></section>
<section class="doc-section"><h2>Regras do processo</h2><div class="content-grid content-grid--3"><article class="rule-card"><h3>Entrada</h3><p>Informe critérios para iniciar o fluxo.</p></article><article class="rule-card"><h3>Decisão</h3><p>Registre aprovações, exceções e alçadas.</p></article><article class="rule-card"><h3>Saída</h3><p>Explique notificações e dados gerados.</p></article></div></section>
<section class="doc-section"><h2>Resumo das etapas</h2><div class="table-wrap"><table><thead><tr><th>Etapa</th><th>Responsável</th><th>Entrada</th><th>Saída</th></tr></thead><tbody><tr><td>Solicitação</td><td>Perfil ou área</td><td>Dados necessários</td><td>Registro criado</td></tr><tr><td>Análise</td><td>Perfil ou área</td><td>Registro pendente</td><td>Decisão registrada</td></tr></tbody></table></div></section>
$html$, 60, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000005', 'FAQ', 'Perguntas frequentes',
   'Organiza atalhos por assunto e dúvidas recorrentes em cartões objetivos.', $html$
<section class="doc-intro"><span class="doc-kicker">Central de dúvidas</span><h2>Perguntas frequentes</h2><p>Reúna respostas curtas e diretas para as dúvidas mais recorrentes.</p></section>
<section class="doc-section"><h2>Acesso rápido</h2><div class="content-grid content-grid--3"><article class="topic-card"><span class="topic-card__icon">01</span><h3>Primeiros passos</h3><p>Dúvidas sobre acesso e configuração inicial.</p></article><article class="topic-card"><span class="topic-card__icon">02</span><h3>Operação</h3><p>Orientações para as tarefas do dia a dia.</p></article><article class="topic-card"><span class="topic-card__icon">03</span><h3>Erros comuns</h3><p>Soluções rápidas para situações conhecidas.</p></article></div></section>
<section class="doc-section faq-list"><article class="faq-item"><h3>Quando devo utilizar esta funcionalidade?</h3><p>Responda objetivamente e indique o cenário recomendado.</p></article><article class="faq-item"><h3>O que fazer quando a operação não for concluída?</h3><p>Indique verificações iniciais e quando acionar o suporte.</p></article><article class="faq-item"><h3>Posso desfazer esta ação?</h3><p>Explique se a operação é reversível e quais cuidados tomar.</p></article></section>
$html$, 70, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000006', 'SOLUCAO_PROBLEMAS', 'Solução de problemas',
   'Relaciona sintomas, causas, diagnóstico visual e ações corretivas.', $html$
<section class="doc-intro"><span class="doc-kicker">Diagnóstico e solução</span><h2>Problema: [sintoma principal]</h2><p>Descreva a mensagem, o comportamento observado e em qual etapa ocorre.</p></section>
<div class="objective-card"><p><strong>Antes de investigar</strong></p><p>Registre versão, usuário, horário e evidências necessárias para reproduzir o problema.</p></div>
<section class="doc-section"><div class="content-grid content-grid--2"><article class="rule-card"><h2>Sintomas</h2><ul><li>Mensagem apresentada.</li><li>Comportamento inesperado.</li><li>Condição em que acontece.</li></ul></article><article class="rule-card"><h2>Possíveis causas</h2><ul><li>Permissão ou configuração.</li><li>Dado inválido ou incompleto.</li><li>Indisponibilidade de integração.</li></ul></article></div></section>
<section class="doc-section"><h2>Como resolver</h2><ol class="flow-strip"><li><strong>Verificar</strong><span>Confirme dados e acessos.</span></li><li><strong>Corrigir</strong><span>Aplique a orientação.</span></li><li><strong>Testar</strong><span>Repita a operação.</span></li><li><strong>Registrar</strong><span>Documente o resultado.</span></li></ol></section>
<section class="doc-section"><h2>Evidência visual</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura da mensagem ou comportamento</strong><span>Oculte dados pessoais e destaque a área relevante.</span></div><figcaption>Exemplo do problema e do ponto que deve ser verificado.</figcaption></figure></section>
<div class="warning"><strong>Quando acionar o suporte</strong><p>Informe os critérios de escalonamento e quais evidências devem ser enviadas.</p></div>
$html$, 80, TRUE, 'migration', 'migration')
ON CONFLICT (codigo) DO UPDATE SET
  nome = EXCLUDED.nome,
  descricao = EXCLUDED.descricao,
  conteudo_html = EXCLUDED.conteudo_html,
  ordem = EXCLUDED.ordem,
  ativo = TRUE,
  updated_at = now(),
  updated_by = 'migration';
