-- Modelos operacionais para documentar inclusão, edição e listagem com evidência visual.
INSERT INTO tb_pagina_template
  (id, codigo, nome, descricao, conteudo_html, ordem, ativo, created_by, updated_by)
VALUES
  ('10000000-0000-0000-0000-000000000018', 'INCLUIR_REGISTRO', 'Incluir registro',
   'Guia de inclusão com pré-requisitos, imagem da tela, descrição de campos e validação final.', $html$
<section class="doc-intro"><span class="doc-kicker">Operação · Inclusão</span><h2>Como incluir [nome do registro]</h2><p>Explique qual informação será cadastrada, quem pode realizar a operação e onde ela será usada.</p></section>
<div class="objective-card"><p><strong>Antes de começar</strong></p><p>Informe permissões, dados obrigatórios e dependências necessárias para criar o registro.</p></div>
<section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura da tela de inclusão</strong><span>Mostre os campos obrigatórios e o botão para salvar.</span></div><figcaption>Tela usada para incluir um novo registro.</figcaption></figure></section>
<section class="doc-section"><h2>Descrição dos campos</h2><div class="table-wrap"><table><thead><tr><th>Campo</th><th>Obrigatório</th><th>Como preencher</th><th>Exemplo</th></tr></thead><tbody><tr><td>Campo principal</td><td>Sim</td><td>Descreva formato, limite e regra de preenchimento.</td><td>Exemplo válido</td></tr><tr><td>Campo complementar</td><td>Não</td><td>Explique quando deve ser informado.</td><td>Exemplo</td></tr><tr><td>Status ou opção</td><td>Conforme regra</td><td>Descreva os valores disponíveis e seu efeito.</td><td>Ativo</td></tr></tbody></table></div></section>
<section class="doc-section"><h2>Passo a passo</h2><ol><li>Acesse o menu e selecione <strong>Novo</strong>.</li><li>Preencha os campos obrigatórios.</li><li>Revise as informações e selecione <strong>Salvar</strong>.</li><li>Confirme a mensagem de sucesso e localize o registro na lista.</li></ol></section>
<div class="result-card"><strong>Resultado esperado</strong><p>Descreva como confirmar que o registro foi incluído corretamente.</p></div>
$html$, 95, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000019', 'EDITAR_REGISTRO', 'Editar registro',
   'Guia de alteração com imagem da tela, campos editáveis, restrições e confirmação.', $html$
<section class="doc-intro"><span class="doc-kicker">Operação · Edição</span><h2>Como editar [nome do registro]</h2><p>Explique quando um registro deve ser alterado e quais dados não podem ser modificados.</p></section>
<section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura da tela de edição</strong><span>Destaque dados atuais, campos editáveis e a ação de salvar.</span></div><figcaption>Tela de edição do registro.</figcaption></figure></section>
<section class="doc-section"><h2>Campos e regras de alteração</h2><div class="table-wrap"><table><thead><tr><th>Campo</th><th>Pode editar?</th><th>Regra</th><th>Impacto</th></tr></thead><tbody><tr><td>Identificação</td><td>Conforme regra</td><td>Informe se o valor é único ou bloqueado após a criação.</td><td>Explique o efeito nos registros relacionados.</td></tr><tr><td>Dados principais</td><td>Sim</td><td>Descreva validações e limites.</td><td>Atualiza a informação exibida no sistema.</td></tr><tr><td>Status</td><td>Conforme permissão</td><td>Explique transições permitidas.</td><td>Pode habilitar ou bloquear o uso do registro.</td></tr></tbody></table></div></section>
<section class="doc-section"><h2>Como editar</h2><ol class="flow-strip"><li><strong>Localizar</strong><span>Encontre o registro na lista.</span></li><li><strong>Abrir</strong><span>Selecione a ação de editar.</span></li><li><strong>Alterar</strong><span>Atualize apenas os campos necessários.</span></li><li><strong>Salvar</strong><span>Confirme a alteração.</span></li></ol></section>
<div class="warning"><strong>Atenção</strong><p>Descreva impactos, auditoria, permissões ou situações em que a alteração não deve ser feita.</p></div>
$html$, 96, TRUE, 'migration', 'migration'),

  ('10000000-0000-0000-0000-000000000020', 'LISTAR_REGISTROS', 'Listar e consultar registros',
   'Guia de consulta com imagem, filtros, descrição de colunas, ações e exportação.', $html$
<section class="doc-intro"><span class="doc-kicker">Operação · Consulta</span><h2>Como consultar [nome dos registros]</h2><p>Explique quais informações podem ser encontradas na lista e em quais cenários ela deve ser usada.</p></section>
<section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Insira a captura da tela de listagem</strong><span>Mostre filtros, tabela, paginação e ações disponíveis.</span></div><figcaption>Lista de registros e filtros de consulta.</figcaption></figure></section>
<section class="doc-section"><h2>Como pesquisar</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Filtros</h3><p>Descreva cada filtro, combinações permitidas e valores padrão.</p></article><article class="rule-card"><h3>Ordenação e paginação</h3><p>Explique como ordenar resultados e navegar entre páginas.</p></article></div></section>
<section class="doc-section"><h2>Descrição das colunas</h2><div class="table-wrap"><table><thead><tr><th>Coluna</th><th>O que informa</th><th>Como interpretar</th></tr></thead><tbody><tr><td>Identificação</td><td>Código ou nome do registro.</td><td>Use para localizar o item correto.</td></tr><tr><td>Status</td><td>Situação atual do registro.</td><td>Explique os possíveis valores e cores.</td></tr><tr><td>Atualização</td><td>Data e responsável pela última alteração.</td><td>Use para conferir a vigência da informação.</td></tr><tr><td>Ações</td><td>Operações disponíveis na linha.</td><td>Detalhe visualizar, editar, excluir e exportar.</td></tr></tbody></table></div></section>
<section class="doc-section"><h2>Ações disponíveis</h2><div class="annotation-grid"><article class="annotation-card"><h3>Visualizar</h3><p>Explique o que pode ser consultado no detalhe.</p></article><article class="annotation-card"><h3>Editar</h3><p>Informe quem pode alterar e para onde a ação direciona.</p></article><article class="annotation-card"><h3>Exportar</h3><p>Descreva formato, filtros considerados e cuidados com os dados.</p></article></div></section>
$html$, 97, TRUE, 'migration', 'migration')
ON CONFLICT (codigo) DO UPDATE SET
  nome = EXCLUDED.nome,
  descricao = EXCLUDED.descricao,
  conteudo_html = EXCLUDED.conteudo_html,
  ordem = EXCLUDED.ordem,
  ativo = TRUE,
  updated_at = now(),
  updated_by = 'migration';
