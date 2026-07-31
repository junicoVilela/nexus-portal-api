-- Renomeia e redesenha os modelos antes vinculados ao DTEC Suite.
-- Passam a ser estruturas genéricas (sem nomes de produto ou telas existentes).

UPDATE tb_pagina_template SET
  codigo = 'LAB_FILTROS',
  nome = 'Laboratório de filtros',
  descricao = 'Critérios, combinações, triagem de resultados e ações recomendadas.',
  conteudo_html = $html$
<section class="doc-intro"><span class="doc-kicker">Pesquisa com critérios</span><h2>Consultar [assunto]</h2><p>Documente a combinação de filtros, a leitura dos resultados e a decisão que cada item exige.</p></section>
<section class="doc-section"><h2>Cenários de uso</h2><div class="filter-chips"><p><span class="filter-chip filter-chip--active">Uso do dia</span><span class="filter-chip">Revisão periódica</span><span class="filter-chip">Busca pontual</span><span class="filter-chip">Retorno de tratamento</span></p></div></section>
<section class="doc-section"><h2>Receita de critérios</h2><div class="table-wrap"><table class="criteria-table"><thead><tr><th>Filtro</th><th>Quando usar</th><th>Exemplo</th><th>Combina com</th></tr></thead><tbody><tr><td><strong>Contexto / grupo</strong></td><td>Delimitar o escopo da consulta</td><td>Grupo ou fila de trabalho</td><td>Período, status</td></tr><tr><td><strong>Identificador</strong></td><td>Localizar um registro específico</td><td>Código, documento ou nome</td><td>Contexto</td></tr><tr><td><strong>Período</strong></td><td>Recortar a janela temporal</td><td>Últimos 7 dias</td><td>Status, categoria</td></tr><tr><td><strong>Status</strong></td><td>Separar pendentes de concluídos</td><td>Pendente</td><td>Contexto, responsável</td></tr></tbody></table></div></section>
<section class="doc-section"><h2>Triagem dos resultados</h2><div class="triage-board"><article class="triage-panel triage-panel--queue"><h3>Itens encontrados</h3><ul class="resource-list resource-list--large"><li class="resource-item"><span class="number-badge">1</span><span><strong>Item A</strong><small>Identificação, categoria e prioridade</small></span><span class="resource-item__meta">Abrir</span></li><li class="resource-item"><span class="number-badge">2</span><span><strong>Item B</strong><small>Identificação, categoria e prioridade</small></span><span class="resource-item__meta">Abrir</span></li></ul></article><article class="triage-panel triage-panel--action"><h3>Ação recomendada</h3><ul class="checklist"><li>Abrir o detalhe quando houver dúvida no resultado.</li><li>Registrar o tratamento quando a evidência for suficiente.</li><li>Salvar a pesquisa se o conjunto de filtros for recorrente.</li></ul></article></div></section>
<div class="warning"><strong>Dados sensíveis</strong><p>Oculte informações pessoais em capturas compartilhadas. Informe quais filtros são obrigatórios nesta operação.</p></div>
$html$,
  ordem = 90,
  ativo = TRUE,
  updated_at = now(),
  updated_by = 'migration'
WHERE codigo IN ('DTEC_ALERTAS', 'LAB_FILTROS');

UPDATE tb_pagina_template SET
  codigo = 'PAINEL_METRICAS',
  nome = 'Painel de métricas',
  descricao = 'Configurar execução, acompanhar status, ler indicadores e validar o resultado.',
  conteudo_html = $html$
<section class="doc-intro"><span class="doc-kicker">Painel de execução</span><h2>Acompanhar uma execução de [processo]</h2><p>Trate a operação como um lote: configure a entrada, acompanhe o andamento e interprete as métricas finais.</p></section>
<section class="doc-section"><h2>Métricas do lote</h2><div class="metric-board"><article class="metric-card"><span class="metric-card__label">Itens processados</span><strong class="metric-card__value">—</strong><p>Total analisado na execução.</p></article><article class="metric-card"><span class="metric-card__label">Itens apontados</span><strong class="metric-card__value">—</strong><p>Quantidade com resultado relevante.</p></article><article class="metric-card"><span class="metric-card__label">Regras / critérios</span><strong class="metric-card__value">—</strong><p>Conjunto usado na execução.</p></article><article class="metric-card"><span class="metric-card__label">Tempo</span><strong class="metric-card__value">—</strong><p>Duração até a conclusão.</p></article></div></section>
<section class="doc-section"><h2>Linha do tempo do status</h2><ol class="pipeline-track"><li class="pipeline-step pipeline-step--ready"><strong>Solicitada</strong><span>Lote criado e aguardando fila.</span></li><li class="pipeline-step pipeline-step--progress"><strong>Em processamento</strong><span>Critérios sendo aplicados aos itens.</span></li><li class="pipeline-step"><strong>Concluída</strong><span>Métricas e resultados disponíveis.</span></li><li class="pipeline-step pipeline-step--risk"><strong>Falha</strong><span>Registrar causa e reprocessar se necessário.</span></li></ol></section>
<section class="doc-section doc-section--soft"><h2>Configuração da execução</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Entrada</h3><ul><li>Filtros ou lote de origem.</li><li>Regras, critérios ou ponto de corte.</li><li>Janela temporal considerada.</li></ul></article><article class="rule-card"><h3>Saída esperada</h3><ul><li>Resultados gerados para análise.</li><li>Indicadores do lote.</li><li>Base para comparar com o esperado.</li></ul></article></div></section>
<div class="result-card"><strong>Validação final</strong><p>Confirme status concluído, métricas coerentes com a configuração e se os apontamentos merecem análise detalhada.</p></div>
$html$,
  ordem = 91,
  ativo = TRUE,
  updated_at = now(),
  updated_by = 'migration'
WHERE codigo IN ('DTEC_SIMULACAO', 'PAINEL_METRICAS');

UPDATE tb_pagina_template SET
  codigo = 'DOSSIE_DECISAO',
  nome = 'Dossiê de decisão',
  descricao = 'Identificação do caso, evidências, matriz de parecer e próximos passos.',
  conteudo_html = $html$
<section class="doc-intro"><span class="doc-kicker">Dossiê de decisão</span><h2>Analisar um [caso]</h2><p>Monte o caso com identificação, evidências e parecer — sem depender de um guia genérico de tela.</p></section>
<section class="doc-section"><div class="dossier-strip"><div><span class="doc-kicker">Identificação</span><h3>Registro / documento</h3><p>Nome, código e origem do apontamento que motivou a análise.</p></div><div><span class="doc-kicker">Contexto</span><h3>Por que abriu</h3><p>Critério, severidade e data do apontamento.</p></div><div><span class="doc-kicker">Responsável</span><h3>Quem decide</h3><p>Perfil autorizado a registrar o parecer neste caso.</p></div></div></section>
<section class="doc-section"><h2>Evidências × decisão</h2><div class="decision-board"><article class="evidence-panel"><h3>Evidências a conferir</h3><ul class="rank-list"><li>Dados apresentados no detalhe.</li><li>Histórico relevante e apontamentos correlatos.</li><li>Critérios que originaram o caso.</li><li>Restrições de sigilo e dados pessoais.</li></ul></article><article class="evidence-panel evidence-panel--decision"><h3>Matriz de parecer</h3><div class="table-wrap"><table><thead><tr><th>Parecer</th><th>Quando aplicar</th><th>Impacto</th></tr></thead><tbody><tr><td><strong>Prosseguir / confirmar</strong></td><td>Evidências sustentam o apontamento</td><td>Descreva o efeito no tratamento</td></tr><tr><td><strong>Descartar</strong></td><td>Não há lastro suficiente</td><td>Registre justificativa</td></tr><tr><td><strong>Escalar</strong></td><td>Exige alçada superior ou outra área</td><td>Indique o próximo responsável</td></tr></tbody></table></div></article></div></section>
<section class="doc-section"><h2>Encerramento do caso</h2><ul class="status-list"><li class="status-item status-item--progress"><span>Parecer registrado</span><strong>Em andamento</strong></li><li class="status-item"><span>Retorno à fila de origem</span><strong>Pendente</strong></li><li class="status-item status-item--done"><span>Caso concluído na análise</span><strong>Feito</strong></li></ul></section>
<div class="callout"><strong>Artigos relacionados</strong><p>Vincule a consulta de origem, a execução que gerou o caso e a regra ou critério aplicado.</p></div>
$html$,
  ordem = 92,
  ativo = TRUE,
  updated_at = now(),
  updated_by = 'migration'
WHERE codigo IN ('DTEC_ANALISE_CLIENTE', 'DOSSIE_DECISAO');

UPDATE tb_pagina_template SET
  codigo = 'ESPECIFICACAO_REGRA',
  nome = 'Especificação de regra',
  descricao = 'Identidade da regra, condições SE/ENTÃO, parâmetros, versão e impacto.',
  conteudo_html = $html$
<section class="doc-intro"><span class="doc-kicker">Especificação de regra</span><h2>Documentar uma regra de [assunto]</h2><p>Descreva a regra como um contrato: quando dispara, o que produz e qual versão está vigente.</p></section>
<section class="doc-section"><div class="spec-card"><div class="spec-card__meta"><span class="status-badge status-badge--required">Versão vigente</span><span class="status-badge">Escopo</span></div><h3>Nome da regra</h3><p>Resuma a finalidade em uma frase e indique processos ou resultados influenciados.</p><div class="spec-card__grid"><div><strong>Código / chave</strong><span>Identificador estável da regra</span></div><div><strong>Owner</strong><span>Área responsável pela manutenção</span></div><div><strong>Última revisão</strong><span>Data e motivação da alteração</span></div></div></div></section>
<section class="doc-section"><h2>Condições SE → ENTÃO</h2><div class="condition-stack"><article class="condition-block"><span class="condition-block__label">SE</span><h3>Condição de entrada</h3><p>Liste critérios, limiares e combinações que disparam a regra.</p></article><article class="condition-block condition-block--then"><span class="condition-block__label">ENTÃO</span><h3>Efeito esperado</h3><p>Descreva pontuação, classificação, notificação ou exclusão do fluxo.</p></article></div></section>
<section class="doc-section"><h2>Parâmetros da regra</h2><div class="table-wrap"><table><thead><tr><th>Parâmetro</th><th>Tipo</th><th>Valor de referência</th><th>Efeito se alterar</th></tr></thead><tbody><tr><td>Limiar / ponto de corte</td><td>Numérico</td><td>Informe o valor vigente</td><td>Muda volume de resultados</td></tr><tr><td>Filtro de elegibilidade</td><td>Lista / flag</td><td>Informe o conjunto</td><td>Inclui ou exclui registros</td></tr><tr><td>Severidade / prioridade</td><td>Enumeração</td><td>Alta / média / baixa</td><td>Prioriza a fila de trabalho</td></tr></tbody></table></div></section>
<section class="doc-section"><h2>Ciclo de vida da versão</h2><ol class="flow-strip"><li><strong>Rascunho</strong><span>Altere sem publicar.</span></li><li><strong>Validar</strong><span>Teste em ambiente controlado.</span></li><li><strong>Publicar</strong><span>Ative a versão.</span></li><li><strong>Monitorar</strong><span>Acompanhe impacto.</span></li></ol></section>
<div class="warning"><strong>Antes de publicar</strong><p>Confirme autorização, testes obrigatórios e plano de rollback para a versão anterior.</p></div>
$html$,
  ordem = 93,
  ativo = TRUE,
  updated_at = now(),
  updated_by = 'migration'
WHERE codigo IN ('DTEC_REGRAS', 'ESPECIFICACAO_REGRA');

UPDATE tb_pagina_template SET
  codigo = 'CATALOGO_PARAMETROS',
  nome = 'Catálogo de parâmetros',
  descricao = 'Grupos, ficha do parâmetro, dependências e impacto da alteração.',
  conteudo_html = $html$
<section class="doc-intro"><span class="doc-kicker">Catálogo corporativo</span><h2>Manter parâmetros de [assunto]</h2><p>Organize o parâmetro como item de catálogo: grupo, ficha, quem consome e o que quebra se mudar.</p></section>
<section class="doc-section"><h2>Grupos do catálogo</h2><div class="content-grid content-grid--3"><article class="topic-card"><span class="topic-card__icon">01</span><h3>Corporativos</h3><p>Dados mestres compartilhados entre módulos.</p></article><article class="topic-card"><span class="topic-card__icon">02</span><h3>Auxiliares</h3><p>Listas de apoio a telas e regras.</p></article><article class="topic-card"><span class="topic-card__icon">03</span><h3>Operacionais</h3><p>Valores que alteram processamentos e rotinas.</p></article></div></section>
<section class="doc-section"><h2>Ficha do parâmetro</h2><div class="table-wrap"><table class="dictionary-table"><thead><tr><th>Atributo</th><th>Conteúdo</th><th>Obrigatório?</th><th>Observação</th></tr></thead><tbody><tr><td><strong>Código</strong></td><td>Identificador estável</td><td><span class="status-badge status-badge--required">Sim</span></td><td>Não reutilize códigos desativados</td></tr><tr><td><strong>Nome</strong></td><td>Rótulo de negócio</td><td><span class="status-badge status-badge--required">Sim</span></td><td>Use linguagem do operador</td></tr><tr><td><strong>Vigência</strong></td><td>Início, fim e status</td><td><span class="status-badge status-badge--required">Sim</span></td><td>Explique ativação e desativação</td></tr><tr><td><strong>Valor / conteúdo</strong></td><td>Dado efetivamente usado</td><td><span class="status-badge">Conforme tipo</span></td><td>Formato, unidade e limites</td></tr></tbody></table></div></section>
<section class="doc-section"><h2>Quem consome este parâmetro</h2><div class="dependency-list"><div class="resource-item"><span class="number-badge">R</span><span><strong>Regras / critérios</strong><small>Quais regras leem este valor</small></span><span class="resource-item__meta">Dependência</span></div><div class="resource-item"><span class="number-badge">P</span><span><strong>Processos</strong><small>Impacto em lotes, filas ou rotinas</small></span><span class="resource-item__meta">Dependência</span></div><div class="resource-item"><span class="number-badge">T</span><span><strong>Telas / relatórios</strong><small>Efeito na classificação, filtros ou exibição</small></span><span class="resource-item__meta">Dependência</span></div></div></section>
<section class="doc-section"><div class="steps"><h2>Manutenção segura</h2><ol><li>Localize o grupo e abra o registro (ou inicie um novo).</li><li>Atualize apenas os atributos necessários na ficha.</li><li>Revise dependências listadas acima.</li><li>Salve e confira vigência na listagem do catálogo.</li></ol></div></section>
<div class="warning"><strong>Impacto operacional</strong><p>Registre processos, filas ou relatórios afetados e se a alteração exige comunicação prévia.</p></div>
$html$,
  ordem = 94,
  ativo = TRUE,
  updated_at = now(),
  updated_by = 'migration'
WHERE codigo IN ('DTEC_PARAMETROS', 'CATALOGO_PARAMETROS');
