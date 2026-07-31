-- Estrutura de exemplo com hierarquia de páginas (nomes genéricos, idempotente).

INSERT INTO tb_projeto (id, nome, slug, descricao, ativo, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000001', 'Documentação de exemplo', 'doc-exemplo',
       'Projeto demonstrativo com hierarquia de páginas para operações.', TRUE, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_projeto WHERE slug = 'doc-exemplo');

INSERT INTO tb_modulo (id, projeto_id, nome, slug, descricao, ordem, ativo, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001',
       'Operações', 'operacoes', 'Módulo de operações de exemplo.', 1, TRUE, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_modulo WHERE slug = 'operacoes'
  AND projeto_id = 'a0000000-0000-4000-8000-000000000001');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002', NULL,
       'Operações', 'exemplo-operacoes', 'EXEMPLO-OPS',
       'Visão geral das operações disponíveis no módulo de exemplo.',
       $html$
<section class="doc-intro"><h2>Operações do módulo</h2><p>Esta página reúne os guias disponíveis para consulta, inclusão e edição de registros no ambiente de demonstração.</p></section>
<section class="doc-section"><h2>Guias disponíveis</h2><div class="resource-list resource-list--large"><article class="resource-item"><span class="number-badge">1</span><span><strong>Lista de registros</strong><small>Consulta e filtros.</small></span><span class="resource-item__meta">EXEMPLO-LISTA</span></article><article class="resource-item"><span class="number-badge">2</span><span><strong>Incluir registro</strong><small>Cadastro de novos itens.</small></span><span class="resource-item__meta">EXEMPLO-INCLUIR</span></article><article class="resource-item"><span class="number-badge">3</span><span><strong>Editar registro</strong><small>Alteração de dados existentes.</small></span><span class="resource-item__meta">EXEMPLO-EDITAR</span></article></div></section>
$html$,
       'RASCUNHO', 0, TRUE, 0, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'exemplo-operacoes');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000002',
       'a0000000-0000-4000-8000-000000000003',
       'Lista de registros', 'exemplo-lista', 'EXEMPLO-LISTA',
       'Como consultar e filtrar registros na tela de listagem.',
       $html$
<section class="doc-intro"><h2>Consultar registros</h2><p>Use os filtros disponíveis para localizar registros por período, status ou identificador. Os resultados aparecem na grade principal da tela.</p></section>
<section class="doc-section"><h2>Filtros úteis</h2><div class="table-wrap"><table><thead><tr><th>Filtro</th><th>Uso</th></tr></thead><tbody><tr><td>Período</td><td>Recorta a janela temporal da consulta.</td></tr><tr><td>Status</td><td>Separa itens pendentes dos concluídos.</td></tr></tbody></table></div></section>
$html$,
       'RASCUNHO', 1, TRUE, 0, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'exemplo-lista');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000002',
       'a0000000-0000-4000-8000-000000000003',
       'Incluir registro', 'exemplo-incluir', 'EXEMPLO-INCLUIR',
       'Passo a passo para cadastrar um novo registro.',
       $html$
<section class="doc-intro"><h2>Novo registro</h2><p>Abra a tela de inclusão, preencha os campos obrigatórios marcados e confirme a operação para gravar o item no sistema.</p></section>
<section class="doc-section"><h2>Pré-requisitos</h2><ul class="checklist"><li>Perfil com permissão de inclusão.</li><li>Dados de referência já cadastrados.</li></ul></section>
<section class="doc-section"><div class="steps"><h2>Passo a passo</h2><ol><li>Acesse o menu de operações.</li><li>Selecione Incluir.</li><li>Preencha os campos e salve.</li></ol></div></section>
<div class="result-card"><strong>Resultado esperado</strong><p>O registro aparece na listagem com status ativo e identificador gerado automaticamente.</p></div>
$html$,
       'RASCUNHO', 2, TRUE, 0, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'exemplo-incluir');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000002',
       'a0000000-0000-4000-8000-000000000003',
       'Editar registro', 'exemplo-editar', 'EXEMPLO-EDITAR',
       'Orientações para alterar um registro existente.',
       $html$
<section class="doc-intro"><h2>Alterar registro</h2><p>Localize o item na listagem, abra o detalhe e atualize os campos necessários antes de confirmar a gravação das alterações.</p></section>
<section class="doc-section"><h2>Campos editáveis</h2><p>Revise cada seção do formulário. Campos somente leitura permanecem bloqueados para edição nesta operação.</p></section>
<section class="doc-section"><div class="screen-frame"><div class="screen-placeholder">Área reservada para captura da tela de edição.</div></div></section>
$html$,
       'RASCUNHO', 3, TRUE, 0, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'exemplo-editar');
