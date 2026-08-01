-- =============================================================================
-- V39: Manual DocFlow — remove Cliente Exemplo e insere documentação completa
-- =============================================================================

-- A) Cleanup Cliente Exemplo (ordem respeita FKs)
DELETE FROM tb_publicacao_changelog
WHERE publicacao_id IN (
  SELECT p.id FROM tb_publicacao p
  INNER JOIN tb_cliente c ON c.id = p.cliente_id
  WHERE c.slug = 'cliente-exemplo' OR c.id = 'a0000000-0000-4000-8000-000000000010'
);

DELETE FROM tb_publicacao
WHERE cliente_id IN (
  SELECT id FROM tb_cliente WHERE slug = 'cliente-exemplo' OR id = 'a0000000-0000-4000-8000-000000000010'
);

DELETE FROM tb_preview_token
WHERE cliente_id IN (
  SELECT id FROM tb_cliente WHERE slug = 'cliente-exemplo' OR id = 'a0000000-0000-4000-8000-000000000010'
);

DELETE FROM tb_cliente_pagina
WHERE cliente_id IN (
  SELECT id FROM tb_cliente WHERE slug = 'cliente-exemplo' OR id = 'a0000000-0000-4000-8000-000000000010'
);

DELETE FROM tb_cliente_modulo
WHERE cliente_id IN (
  SELECT id FROM tb_cliente WHERE slug = 'cliente-exemplo' OR id = 'a0000000-0000-4000-8000-000000000010'
)
   OR id = 'a0000000-0000-4000-8000-000000000012';

DELETE FROM tb_cliente_projeto
WHERE cliente_id IN (
  SELECT id FROM tb_cliente WHERE slug = 'cliente-exemplo' OR id = 'a0000000-0000-4000-8000-000000000010'
)
   OR id = 'a0000000-0000-4000-8000-000000000011';

-- Filhos antes do pai (revisões/anexos cascateiam pelo parent_id)
DELETE FROM tb_pagina WHERE id IN (
  'a0000000-0000-4000-8000-000000000004',
  'a0000000-0000-4000-8000-000000000005',
  'a0000000-0000-4000-8000-000000000006'
);
DELETE FROM tb_pagina WHERE slug IN ('exemplo-lista', 'exemplo-incluir', 'exemplo-editar', 'exemplo-operacoes')
   OR id IN ('a0000000-0000-4000-8000-000000000003');

DELETE FROM tb_modulo
WHERE (slug = 'operacoes' AND projeto_id = 'a0000000-0000-4000-8000-000000000001')
   OR id = 'a0000000-0000-4000-8000-000000000002';

DELETE FROM tb_projeto
WHERE slug = 'doc-exemplo' OR id = 'a0000000-0000-4000-8000-000000000001';

DELETE FROM tb_cliente
WHERE slug = 'cliente-exemplo' OR id = 'a0000000-0000-4000-8000-000000000010';


-- B) Cliente DOCFLOW
INSERT INTO tb_cliente (id, nome, slug, ativo, tema_cor_primaria, tema_cor_fundo, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000100', 'DOCFLOW', 'docflow', TRUE, '#4f46e5', '#f7f8fa', 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_cliente WHERE slug = 'docflow');

INSERT INTO tb_projeto (id, nome, slug, descricao, ativo, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000101', 'Manual DocFlow', 'docflow-manual',
       'Manual oficial do Softon Portal DocFlow — referência para operação editorial e modelo para novos manuais.',
       TRUE, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_projeto WHERE slug = 'docflow-manual');

INSERT INTO tb_modulo (id, projeto_id, nome, slug, descricao, ordem, ativo, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000110', 'a0000000-0000-4000-8000-000000000101', 'Início', 'inicio', 'Visão geral e primeiros passos', 1, TRUE, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_modulo WHERE slug = 'inicio' AND projeto_id = 'a0000000-0000-4000-8000-000000000101');

INSERT INTO tb_modulo (id, projeto_id, nome, slug, descricao, ordem, ativo, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000111', 'a0000000-0000-4000-8000-000000000101', 'Estrutura', 'estrutura', 'Clientes, projetos e módulos', 2, TRUE, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_modulo WHERE slug = 'estrutura' AND projeto_id = 'a0000000-0000-4000-8000-000000000101');

INSERT INTO tb_modulo (id, projeto_id, nome, slug, descricao, ordem, ativo, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000112', 'a0000000-0000-4000-8000-000000000101', 'Conteúdo', 'conteudo', 'Páginas, editor, modelos e blocos', 3, TRUE, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_modulo WHERE slug = 'conteudo' AND projeto_id = 'a0000000-0000-4000-8000-000000000101');

INSERT INTO tb_modulo (id, projeto_id, nome, slug, descricao, ordem, ativo, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000113', 'a0000000-0000-4000-8000-000000000101', 'Revisão', 'revisao', 'Workflow editorial e qualidade', 4, TRUE, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_modulo WHERE slug = 'revisao' AND projeto_id = 'a0000000-0000-4000-8000-000000000101');

INSERT INTO tb_modulo (id, projeto_id, nome, slug, descricao, ordem, ativo, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000114', 'a0000000-0000-4000-8000-000000000101', 'Publicações', 'publicacoes', 'Pacotes, preview e distribuição', 5, TRUE, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_modulo WHERE slug = 'publicacoes' AND projeto_id = 'a0000000-0000-4000-8000-000000000101');

INSERT INTO tb_modulo (id, projeto_id, nome, slug, descricao, ordem, ativo, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000101', 'Referência', 'referencia', 'FAQ, modelos de exemplo e glossário', 6, TRUE, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_modulo WHERE slug = 'referencia' AND projeto_id = 'a0000000-0000-4000-8000-000000000101');

INSERT INTO tb_cliente_projeto (id, cliente_id, projeto_id)
SELECT 'a0000000-0000-4000-8000-000000000301', 'a0000000-0000-4000-8000-000000000100', 'a0000000-0000-4000-8000-000000000101'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_cliente_projeto WHERE cliente_id = 'a0000000-0000-4000-8000-000000000100' AND projeto_id = 'a0000000-0000-4000-8000-000000000101'
);

INSERT INTO tb_cliente_modulo (id, cliente_id, modulo_id)
SELECT 'a0000000-0000-4000-8000-000000000310', 'a0000000-0000-4000-8000-000000000100', 'a0000000-0000-4000-8000-000000000110'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_cliente_modulo WHERE cliente_id = 'a0000000-0000-4000-8000-000000000100' AND modulo_id = 'a0000000-0000-4000-8000-000000000110'
);

INSERT INTO tb_cliente_modulo (id, cliente_id, modulo_id)
SELECT 'a0000000-0000-4000-8000-000000000311', 'a0000000-0000-4000-8000-000000000100', 'a0000000-0000-4000-8000-000000000111'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_cliente_modulo WHERE cliente_id = 'a0000000-0000-4000-8000-000000000100' AND modulo_id = 'a0000000-0000-4000-8000-000000000111'
);

INSERT INTO tb_cliente_modulo (id, cliente_id, modulo_id)
SELECT 'a0000000-0000-4000-8000-000000000312', 'a0000000-0000-4000-8000-000000000100', 'a0000000-0000-4000-8000-000000000112'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_cliente_modulo WHERE cliente_id = 'a0000000-0000-4000-8000-000000000100' AND modulo_id = 'a0000000-0000-4000-8000-000000000112'
);

INSERT INTO tb_cliente_modulo (id, cliente_id, modulo_id)
SELECT 'a0000000-0000-4000-8000-000000000313', 'a0000000-0000-4000-8000-000000000100', 'a0000000-0000-4000-8000-000000000113'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_cliente_modulo WHERE cliente_id = 'a0000000-0000-4000-8000-000000000100' AND modulo_id = 'a0000000-0000-4000-8000-000000000113'
);

INSERT INTO tb_cliente_modulo (id, cliente_id, modulo_id)
SELECT 'a0000000-0000-4000-8000-000000000314', 'a0000000-0000-4000-8000-000000000100', 'a0000000-0000-4000-8000-000000000114'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_cliente_modulo WHERE cliente_id = 'a0000000-0000-4000-8000-000000000100' AND modulo_id = 'a0000000-0000-4000-8000-000000000114'
);

INSERT INTO tb_cliente_modulo (id, cliente_id, modulo_id)
SELECT 'a0000000-0000-4000-8000-000000000315', 'a0000000-0000-4000-8000-000000000100', 'a0000000-0000-4000-8000-000000000115'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_cliente_modulo WHERE cliente_id = 'a0000000-0000-4000-8000-000000000100' AND modulo_id = 'a0000000-0000-4000-8000-000000000115'
);

-- C) Páginas do manual (todas PUBLICADO)

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000200', 'a0000000-0000-4000-8000-000000000110', NULL,
       'Central DocFlow', 'df-central', 'DF-CENTRAL',
       'Ponto de entrada do manual DocFlow com visão geral e links para todos os módulos.',
       $html$
<section class="doc-intro doc-intro--center"><span class="doc-kicker">Softon Portal · DocFlow</span><h2>Central DocFlow</h2><p>Manual oficial de operação editorial do DocFlow — referência para equipes que criam, revisam e publicam documentação no Softon Portal.</p></section>
<div class="objective-card"><p><strong>Objetivo deste manual</strong></p><p>Orientar editores, revisores e administradores na jornada completa: estruturar Cliente, Projeto e Módulo, produzir Páginas com Modelos e Blocos, conduzir Revisão e gerar Publicações para distribuição.</p></div>
<section class="doc-section"><h2>Módulos do manual</h2><div class="resource-list resource-list--large"><article class="resource-item"><span class="number-badge">1</span><span><strong>Início</strong><small>Primeiros passos e conceitos do DocFlow.</small></span><span class="resource-item__meta">DF-CENTRAL</span></article><article class="resource-item"><span class="number-badge">2</span><span><strong>Estrutura</strong><small>Clientes, projetos e módulos.</small></span><span class="resource-item__meta">DF-ESTRUTURA</span></article><article class="resource-item"><span class="number-badge">3</span><span><strong>Conteúdo</strong><small>Páginas, editor, modelos e blocos.</small></span><span class="resource-item__meta">DF-PAGINAS</span></article><article class="resource-item"><span class="number-badge">4</span><span><strong>Revisão</strong><small>Workflow editorial e qualidade.</small></span><span class="resource-item__meta">DF-REVISAO</span></article><article class="resource-item"><span class="number-badge">5</span><span><strong>Publicações</strong><small>Pacotes, preview e versionamento.</small></span><span class="resource-item__meta">DF-PUB</span></article><article class="resource-item"><span class="number-badge">6</span><span><strong>Referência</strong><small>FAQ, modelos de exemplo e glossário.</small></span><span class="resource-item__meta">DF-REF</span></article></div></section>
<section class="doc-section"><h2>Boas práticas editoriais</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Antes de publicar</h3><ul class="checklist"><li>Substitua placeholders e imagens de exemplo.</li><li>Confirme código de tela único (<code>codigoTela</code>).</li><li>Revise links internos e referências cruzadas.</li><li>Valide o checklist de qualidade na Central de revisão.</li></ul></article><article class="rule-card"><h3>Para novos manuais</h3><ul class="checklist"><li>Copie a estrutura deste manual como modelo.</li><li>Adapte módulos ao produto documentado.</li><li>Use a seção Referência como vitrine de templates.</li><li>Gere o pacote somente após páginas PUBLICADO.</li></ul></article></div></section>
<div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-START">DF-START</span><span data-codigo-tela="DF-CONCEITOS">DF-CONCEITOS</span><span data-codigo-tela="DF-ESTRUTURA">DF-ESTRUTURA</span></p></div>
$html$,
       'PUBLICADO', 0, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-central');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000201', 'a0000000-0000-4000-8000-000000000110', 'a0000000-0000-4000-8000-000000000200',
       'Primeiros passos', 'df-primeiros-passos', 'DF-START',
       'Onboarding operacional para novos usuários do DocFlow no Softon Portal.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Onboarding</span><h2>Primeiros passos no DocFlow</h2><p>Jornada recomendada para quem começa a documentar no Softon Portal usando o DocFlow.</p></section>
<div class="objective-card"><p><strong>Resultado esperado</strong></p><p>Ao concluir esta trilha você terá estrutura cadastrada, uma página publicada e entenderá o caminho até a primeira Publicação.</p></div>
<section class="doc-section"><h2>Checklist de implantação</h2><ul class="status-list"><li class="status-item status-item--done"><span>Acessar o DocFlow no Softon Portal</span><strong>Concluído</strong></li><li class="status-item status-item--progress"><span>Cadastrar Cliente, Projeto e Módulo</span><strong>Em andamento</strong></li><li class="status-item"><span>Criar página com Modelo ou Kit</span><strong>Pendente</strong></li><li class="status-item"><span>Enviar para revisão e publicar</span><strong>Pendente</strong></li><li class="status-item"><span>Gerar pacote na área Publicações</span><strong>Pendente</strong></li></ul></section>
<section class="doc-section"><div class="steps"><h2>Passo a passo</h2><ol><li><strong>Estruture o manual</strong> — cadastre o Cliente (destinatário), o Projeto (produto documentado) e os Módulos (áreas funcionais).</li><li><strong>Vincule ao cliente</strong> — associe projeto e módulos para que as páginas entrem no pacote.</li><li><strong>Crie uma página</strong> — escolha um Modelo, preencha título, código de tela e conteúdo HTML.</li><li><strong>Revise a qualidade</strong> — resolva itens do checklist antes de enviar para a Central de revisão.</li><li><strong>Publique e gere o pacote</strong> — páginas PUBLICADO elegíveis compõem o ZIP/PDF.</li></ol></div></section>
<section class="doc-section"><h2>Permissões necessárias</h2><div class="table-wrap"><table><thead><tr><th>Perfil</th><th>Atividades</th></tr></thead><tbody><tr><td>Editor</td><td>Criar e editar páginas, enviar para revisão.</td></tr><tr><td>Revisor</td><td>Analisar, aprovar ou devolver na Central de revisão.</td></tr><tr><td>Administrador</td><td>Estrutura, vínculos, publicações e configurações.</td></tr></tbody></table></div></section>
<div class="warning"><strong>Atenção</strong><p>A ordem correta é Cliente → Projeto → Módulo → Página. Pular etapas impede que conteúdo apareça no diagnóstico de publicação.</p></div>
<div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-CONCEITOS">DF-CONCEITOS</span><span data-codigo-tela="DF-CLIENTES">DF-CLIENTES</span><span data-codigo-tela="DF-PAG-CRIAR">DF-PAG-CRIAR</span></p></div>
$html$,
       'PUBLICADO', 1, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-primeiros-passos');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000202', 'a0000000-0000-4000-8000-000000000110', 'a0000000-0000-4000-8000-000000000200',
       'Conceitos do DocFlow', 'df-conceitos', 'DF-CONCEITOS',
       'Glossário dos termos Cliente, Projeto, Módulo, Página, Publicação e fluxo editorial.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Glossário DocFlow</span><h2>Conceitos do DocFlow</h2><p>Termos usados no Softon Portal para organizar e distribuir manuais digitais.</p></section>
<section class="doc-section"><h2>Entidades principais</h2><div class="table-wrap"><table class="dictionary-table"><thead><tr><th>Termo</th><th>Definição</th><th>Exemplo prático</th></tr></thead><tbody>
<tr><td><strong>Cliente</strong></td><td>Destinatário do manual; concentra tema, logo e vínculos.</td><td>Área de negócio que receberá o pacote publicado.</td></tr>
<tr><td><strong>Projeto</strong></td><td>Produto ou sistema documentado.</td><td>Manual DocFlow, Manual do módulo X.</td></tr>
<tr><td><strong>Módulo</strong></td><td>Divisão funcional dentro do projeto.</td><td>Início, Operações, Configurações.</td></tr>
<tr><td><strong>Página</strong></td><td>Unidade de conteúdo HTML com status editorial.</td><td>Guia de listagem, FAQ, passo a passo.</td></tr>
<tr><td><strong>Publicação</strong></td><td>Pacote versionado (ZIP/PDF) gerado para um cliente.</td><td>Manual v1.3 distribuído à operação.</td></tr>
<tr><td><strong>Revisão</strong></td><td>Histórico e fila de análise editorial.</td><td>Central de revisão com aprovação ou devolução.</td></tr>
<tr><td><strong>Modelo</strong></td><td>Template de estrutura HTML do catálogo.</td><td>PRIMEIROS_PASSOS, CONSULTA, FAQ.</td></tr>
<tr><td><strong>Bloco</strong></td><td>Fragmento reutilizável inserido no editor.</td><td>objective-card, steps, checklist.</td></tr>
<tr><td><strong>Kit</strong></td><td>Conjunto de blocos para tipo de página.</td><td>kit-lista, kit-incluir, kit-menu.</td></tr>
<tr><td><strong>Snapshot</strong></td><td>Instantâneo do conteúdo no momento da publicação.</td><td>Árvore de páginas congelada no pacote.</td></tr>
<tr><td><strong>Preview token</strong></td><td>Link temporário para pré-visualizar o manual.</td><td>Token com validade para stakeholders.</td></tr>
</tbody></table></div></section>
<section class="doc-section"><h2>Status de uma página</h2><ol class="flow-strip"><li><strong>RASCUNHO</strong><span>Edição livre pelo autor.</span></li><li><strong>EM_REVISAO</strong><span>Na fila da Central de revisão.</span></li><li><strong>APROVADO</strong><span>Conteúdo validado, aguardando publicação.</span></li><li><strong>PUBLICADO</strong><span>Elegível para o pacote.</span></li><li><strong>ARQUIVADO</strong><span>Fora do manual ativo.</span></li></ol></section>
<div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-START">DF-START</span><span data-codigo-tela="DF-REVISAO">DF-REVISAO</span><span data-codigo-tela="DF-PUB">DF-PUB</span></p></div>
$html$,
       'PUBLICADO', 2, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-conceitos');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000210', 'a0000000-0000-4000-8000-000000000111', NULL,
       'Estrutura do manual', 'df-estrutura', 'DF-ESTRUTURA',
       'Índice dos guias sobre Cliente, Projeto e Módulo.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Estrutura</span><h2>Estrutura do manual</h2><p>Como organizar Cliente, Projeto e Módulo antes de criar páginas.</p></section><div class="objective-card"><p><strong>Objetivo</strong></p><p>Montar a hierarquia editorial que determina o que entra em cada Publicação.</p></div><section class="doc-section"><h2>Guias disponíveis</h2><div class="resource-list resource-list--large"><article class="resource-item"><span class="number-badge">1</span><span><strong>Clientes</strong><small>Destinatário, tema e vínculos.</small></span><span class="resource-item__meta">DF-CLIENTES</span></article><article class="resource-item"><span class="number-badge">2</span><span><strong>Projetos</strong><small>Produto ou sistema documentado.</small></span><span class="resource-item__meta">DF-PROJETOS</span></article><article class="resource-item"><span class="number-badge">3</span><span><strong>Módulos</strong><small>Áreas funcionais e ordem de navegação.</small></span><span class="resource-item__meta">DF-MODULOS</span></article></div></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-CLIENTES">DF-CLIENTES</span><span data-codigo-tela="DF-PROJETOS">DF-PROJETOS</span><span data-codigo-tela="DF-MODULOS">DF-MODULOS</span></p></div>
$html$,
       'PUBLICADO', 0, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-estrutura');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000211', 'a0000000-0000-4000-8000-000000000111', 'a0000000-0000-4000-8000-000000000210',
       'Clientes', 'df-clientes', 'DF-CLIENTES',
       'Cadastro de clientes, tema, logo e vínculos editoriais.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Cliente</span><h2>Clientes no DocFlow</h2><p>O Cliente representa quem recebe o manual publicado e define identidade visual.</p></section><div class="objective-card"><p><strong>Objetivo</strong></p><p>Cadastrar e vincular clientes para que páginas elegíveis componham o pacote correto.</p></div><section class="doc-section"><h2>O que é um Cliente</h2><p>No DocFlow, Cliente não é o usuário logado — é o <strong>destinatário do manual</strong>: unidade de negócio, produto interno ou área que consumirá a documentação.</p></section><section class="doc-section"><h2>Campos e recursos</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Nome e slug</h3><p>Identificação humana e URL amigável do manual.</p></article><article class="rule-card"><h3>Tema</h3><p>Cores primária e de fundo aplicadas no pacote PWA.</p></article><article class="rule-card"><h3>Logo</h3><p>Marca exibida na capa e navegação do manual.</p></article><article class="rule-card"><h3>Vínculos</h3><p>Projetos, módulos e páginas associados ao cliente.</p></article></div></section><section class="doc-section"><h2>Quando usar</h2><ul class="checklist"><li>Um manual por cliente ou por linha de produto.</li><li>Vários projetos no mesmo cliente quando compartilham público.</li><li>Tema e logo distintos por unidade de negócio.</li></ul></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PROJETOS">DF-PROJETOS</span><span data-codigo-tela="DF-MODULOS">DF-MODULOS</span></p></div>
$html$,
       'PUBLICADO', 1, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-clientes');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000212', 'a0000000-0000-4000-8000-000000000111', 'a0000000-0000-4000-8000-000000000210',
       'Projetos', 'df-projetos', 'DF-PROJETOS',
       'Como criar e vincular projetos documentais.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Projeto</span><h2>Projetos no DocFlow</h2><p>Agrupam módulos e páginas de um mesmo produto ou sistema.</p></section><div class="objective-card"><p><strong>Objetivo</strong></p><p>Representar o escopo documental — por exemplo, Manual DocFlow ou Manual do Release Orchestrator.</p></div><section class="doc-section"><h2>Características</h2><div class="annotation-grid annotation-grid--2"><article class="annotation-card"><h3>Nome e slug</h3><p>Identificação única no catálogo editorial.</p></article><article class="annotation-card"><h3>Descrição</h3><p>Contexto para editores e revisores.</p></article><article class="annotation-card"><h3>Módulos filhos</h3><p>Organizam páginas por área funcional.</p></article><article class="annotation-card"><h3>Vínculo com cliente</h3><p>Necessário para elegibilidade na publicação.</p></article></div></section><div class="steps"><h2>Passo a passo</h2><ol><li>Acesse DocFlow → Projetos → Novo projeto.</li><li>Informe nome, slug e descrição.</li><li>Cadastre módulos ou vincule módulos existentes.</li><li>Associe o projeto ao cliente destinatário.</li></ol></div><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-CLIENTES">DF-CLIENTES</span><span data-codigo-tela="DF-MODULOS">DF-MODULOS</span></p></div>
$html$,
       'PUBLICADO', 2, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-projetos');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000213', 'a0000000-0000-4000-8000-000000000111', 'a0000000-0000-4000-8000-000000000210',
       'Módulos', 'df-modulos', 'DF-MODULOS',
       'Organização funcional e ordenação de módulos.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Módulo</span><h2>Módulos no DocFlow</h2><p>Segmentam o manual em seções navegáveis com ordem definida.</p></section><div class="objective-card"><p><strong>Objetivo</strong></p><p>Facilitar a localização de conteúdo pelo leitor final e pelo editor.</p></div><section class="doc-section"><h2>Boas práticas de nomenclatura</h2><ul class="checklist"><li>Use nomes reconhecíveis pela operação (Início, Operações, Configurações).</li><li>Mantenha entre 3 e 8 módulos por projeto.</li><li>Defina ordem numérica coerente com a jornada do usuário.</li><li>Evite módulos genéricos demais ('Outros', 'Diversos').</li></ul></section><section class="doc-section"><h2>Relação com páginas</h2><p>Toda página pertence a um módulo. Páginas pai (índice ou menu) organizam subpáginas por <code>parentId</code> e <code>ordem</code>.</p></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAGINAS">DF-PAGINAS</span><span data-codigo-tela="DF-PAG-HIERARQUIA">DF-PAG-HIERARQUIA</span></p></div>
$html$,
       'PUBLICADO', 3, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-modulos');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000220', 'a0000000-0000-4000-8000-000000000112', NULL,
       'Páginas e modelos', 'df-paginas', 'DF-PAGINAS',
       'Índice dos guias sobre produção de conteúdo no DocFlow.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Conteúdo</span><h2>Páginas e modelos</h2><p>Produção editorial: listagem, criação, edição, modelos, blocos e hierarquia.</p></section><section class="doc-section"><h2>Guias disponíveis</h2><div class="resource-list resource-list--large"><article class="resource-item"><span class="number-badge">1</span><span><strong>Listar páginas</strong><small>Consulta, filtros e ações na grade de páginas.</small></span><span class="resource-item__meta">DF-PAG-LISTA</span></article><article class="resource-item"><span class="number-badge">2</span><span><strong>Criar página</strong><small>Wizard Nova por tipo: lista, incluir, editar, índice, menu.</small></span><span class="resource-item__meta">DF-PAG-CRIAR</span></article><article class="resource-item"><span class="number-badge">3</span><span><strong>Editar página</strong><small>Editor Rico, Código, Split e Prévia com autosave.</small></span><span class="resource-item__meta">DF-PAG-EDITAR</span></article><article class="resource-item"><span class="number-badge">4</span><span><strong>Modelos de página</strong><small>Catálogo dos 20 templates do sistema.</small></span><span class="resource-item__meta">DF-PAG-MODELOS</span></article><article class="resource-item"><span class="number-badge">5</span><span><strong>Biblioteca de blocos e kits</strong><small>Blocos, kits lista/incluir/editar/índice/menu.</small></span><span class="resource-item__meta">DF-PAG-BLOCOS</span></article><article class="resource-item"><span class="number-badge">6</span><span><strong>Hierarquia e subpáginas</strong><small>parentId, drag-drop e páginas pasta/menu.</small></span><span class="resource-item__meta">DF-PAG-HIERARQUIA</span></article></div></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAG-CRIAR">DF-PAG-CRIAR</span><span data-codigo-tela="DF-PAG-EDITAR">DF-PAG-EDITAR</span></p></div>
$html$,
       'PUBLICADO', 0, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-paginas');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000221', 'a0000000-0000-4000-8000-000000000112', 'a0000000-0000-4000-8000-000000000220',
       'Listar páginas', 'df-paginas-lista', 'DF-PAG-LISTA',
       'Filtros e ações na listagem de páginas do DocFlow.',
       $html$
<section class="doc-intro"><span class="doc-kicker">DocFlow · Consulta</span><h2>Listar páginas</h2><p>Tela de listagem de páginas com busca, filtros por status, projeto e módulo.</p></section><div class="objective-card"><p><strong>Objetivo</strong></p><p>Localizar páginas para editar, revisar ou publicar.</p></div><section class="doc-section"><h2>Filtros disponíveis</h2><div class="table-wrap"><table class="criteria-table"><thead><tr><th>Filtro</th><th>Uso</th><th>Exemplo</th></tr></thead><tbody><tr><td><strong>Busca textual</strong></td><td>Título, slug ou código de tela</td><td>DF-PAG</td></tr><tr><td><strong>Status</strong></td><td>RASCUNHO, EM_REVISAO, PUBLICADO</td><td>PUBLICADO</td></tr><tr><td><strong>Projeto</strong></td><td>Recorta por projeto documental</td><td>Manual DocFlow</td></tr><tr><td><strong>Módulo</strong></td><td>Área funcional da página</td><td>Conteúdo</td></tr></tbody></table></div></section><section class="doc-section"><h2>Ações da grade</h2><p>Abrir edição, enviar para revisão, visualizar histórico e arquivar conforme permissão do perfil.</p></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAG-EDITAR">DF-PAG-EDITAR</span><span data-codigo-tela="DF-REV-ENVIAR">DF-REV-ENVIAR</span></p></div>
$html$,
       'PUBLICADO', 1, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-paginas-lista');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000222', 'a0000000-0000-4000-8000-000000000112', 'a0000000-0000-4000-8000-000000000220',
       'Criar página', 'df-paginas-criar', 'DF-PAG-CRIAR',
       'Wizard de criação com tipos e modelos.',
       $html$
<section class="doc-intro"><span class="doc-kicker">DocFlow · Inclusão</span><h2>Criar página</h2><p>Assistente Nova página com seleção de tipo e modelo.</p></section><div class="steps"><h2>Passo a passo</h2><ol><li>Em Páginas, clique em Nova página.</li><li>Escolha o tipo: lista, incluir, editar, índice ou menu.</li><li>Selecione um Modelo do catálogo (opcional).</li><li>Informe módulo, título, slug e código de tela únicos.</li><li>Preencha resumo e conteúdo; salve como RASCUNHO.</li></ol></div><section class="doc-section"><h2>Tipos de página</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Lista / incluir / editar</h3><p>Kits pré-montados para operações CRUD.</p></article><article class="rule-card"><h3>Índice / menu</h3><p>Páginas pai que agrupam subpáginas.</p></article></div></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAG-MODELOS">DF-PAG-MODELOS</span><span data-codigo-tela="DF-PAG-BLOCOS">DF-PAG-BLOCOS</span></p></div>
$html$,
       'PUBLICADO', 2, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-paginas-criar');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000223', 'a0000000-0000-4000-8000-000000000112', 'a0000000-0000-4000-8000-000000000220',
       'Editar página', 'df-paginas-editar', 'DF-PAG-EDITAR',
       'Editor, autosave e modos de visualização.',
       $html$
<section class="doc-intro"><span class="doc-kicker">DocFlow · Edição</span><h2>Editar página</h2><p>Editor com modos Rico, Código, Split e Prévia; autosave preserva alterações.</p></section><section class="doc-section"><h2>Modos do editor</h2><div class="annotation-grid annotation-grid--2"><article class="annotation-card"><h3>Rico</h3><p>Edição visual com blocos e formatação.</p></article><article class="annotation-card"><h3>Código</h3><p>HTML direto com classes do design system.</p></article><article class="annotation-card"><h3>Split</h3><p>Rico e código lado a lado.</p></article><article class="annotation-card"><h3>Prévia</h3><p>Renderização como no manual publicado.</p></article></div></section><section class="doc-section"><h2>Autosave e anexos</h2><p>Alterações são salvas automaticamente. Use a Biblioteca de mídia para imagens e anexe arquivos na página quando necessário.</p></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAG-BLOCOS">DF-PAG-BLOCOS</span><span data-codigo-tela="DF-REV-QUALIDADE">DF-REV-QUALIDADE</span></p></div>
$html$,
       'PUBLICADO', 3, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-paginas-editar');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000224', 'a0000000-0000-4000-8000-000000000112', 'a0000000-0000-4000-8000-000000000220',
       'Modelos de página', 'df-paginas-modelos', 'DF-PAG-MODELOS',
       'Os 20 templates do sistema DocFlow.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Modelos</span><h2>Modelos de página</h2><p>Os 20 templates do sistema disponíveis ao criar uma nova página.</p></section><section class="doc-section"><h2>Catálogo completo</h2><div class="resource-list resource-list--large"><article class="resource-item"><span class="number-badge">1</span><span><strong>Guia de funcionalidade</strong><small>Código: FUNCIONALIDADE</small></span><span class="resource-item__meta">FUNCIONALIDADE</span></article><article class="resource-item"><span class="number-badge">2</span><span><strong>Passo a passo visual</strong><small>Código: PASSO_A_PASSO</small></span><span class="resource-item__meta">PASSO_A_PASSO</span></article><article class="resource-item"><span class="number-badge">3</span><span><strong>Cadastro ou edição</strong><small>Código: CADASTRO</small></span><span class="resource-item__meta">CADASTRO</span></article><article class="resource-item"><span class="number-badge">4</span><span><strong>Consulta ou listagem</strong><small>Código: CONSULTA</small></span><span class="resource-item__meta">CONSULTA</span></article><article class="resource-item"><span class="number-badge">5</span><span><strong>Perguntas frequentes</strong><small>Código: FAQ</small></span><span class="resource-item__meta">FAQ</span></article><article class="resource-item"><span class="number-badge">6</span><span><strong>Solução de problemas</strong><small>Código: SOLUCAO_PROBLEMAS</small></span><span class="resource-item__meta">SOLUCAO_PROBLEMAS</span></article><article class="resource-item"><span class="number-badge">7</span><span><strong>Dicionário de campos</strong><small>Código: DICIONARIO_CAMPOS</small></span><span class="resource-item__meta">DICIONARIO_CAMPOS</span></article><article class="resource-item"><span class="number-badge">8</span><span><strong>Fluxo de processo</strong><small>Código: PROCESSO</small></span><span class="resource-item__meta">PROCESSO</span></article><article class="resource-item"><span class="number-badge">9</span><span><strong>Central de ajuda</strong><small>Código: CENTRAL_AJUDA</small></span><span class="resource-item__meta">CENTRAL_AJUDA</span></article><article class="resource-item"><span class="number-badge">10</span><span><strong>Índice de categoria</strong><small>Código: CATEGORIA_ARTIGOS</small></span><span class="resource-item__meta">CATEGORIA_ARTIGOS</span></article><article class="resource-item"><span class="number-badge">11</span><span><strong>Primeiros passos</strong><small>Código: PRIMEIROS_PASSOS</small></span><span class="resource-item__meta">PRIMEIROS_PASSOS</span></article><article class="resource-item"><span class="number-badge">12</span><span><strong>Guia de relatório</strong><small>Código: RELATORIO</small></span><span class="resource-item__meta">RELATORIO</span></article><article class="resource-item"><span class="number-badge">13</span><span><strong>Incluir registro</strong><small>Código: INCLUIR_REGISTRO</small></span><span class="resource-item__meta">INCLUIR_REGISTRO</span></article><article class="resource-item"><span class="number-badge">14</span><span><strong>Editar registro</strong><small>Código: EDITAR_REGISTRO</small></span><span class="resource-item__meta">EDITAR_REGISTRO</span></article><article class="resource-item"><span class="number-badge">15</span><span><strong>Listar e consultar registros</strong><small>Código: LISTAR_REGISTROS</small></span><span class="resource-item__meta">LISTAR_REGISTROS</span></article><article class="resource-item"><span class="number-badge">16</span><span><strong>Laboratório de filtros</strong><small>Código: LAB_FILTROS</small></span><span class="resource-item__meta">LAB_FILTROS</span></article><article class="resource-item"><span class="number-badge">17</span><span><strong>Painel de métricas</strong><small>Código: PAINEL_METRICAS</small></span><span class="resource-item__meta">PAINEL_METRICAS</span></article><article class="resource-item"><span class="number-badge">18</span><span><strong>Dossiê de decisão</strong><small>Código: DOSSIE_DECISAO</small></span><span class="resource-item__meta">DOSSIE_DECISAO</span></article><article class="resource-item"><span class="number-badge">19</span><span><strong>Especificação de regra</strong><small>Código: ESPECIFICACAO_REGRA</small></span><span class="resource-item__meta">ESPECIFICACAO_REGRA</span></article><article class="resource-item"><span class="number-badge">20</span><span><strong>Catálogo de parâmetros</strong><small>Código: CATALOGO_PARAMETROS</small></span><span class="resource-item__meta">CATALOGO_PARAMETROS</span></article></div></section><section class="doc-section"><h2>Como escolher</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Conteúdo orientado a tela</h3><p>FUNCIONALIDADE, CONSULTA, CADASTRO.</p></article><article class="rule-card"><h3>Procedimentos</h3><p>PASSO_A_PASSO, INCLUIR_REGISTRO, EDITAR_REGISTRO.</p></article><article class="rule-card"><h3>Navegação</h3><p>CENTRAL_AJUDA, CATEGORIA_ARTIGOS, PRIMEIROS_PASSOS.</p></article></div></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAG-CRIAR">DF-PAG-CRIAR</span><span data-codigo-tela="DF-REF">DF-REF</span></p></div>
$html$,
       'PUBLICADO', 4, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-paginas-modelos');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000225', 'a0000000-0000-4000-8000-000000000112', 'a0000000-0000-4000-8000-000000000220',
       'Biblioteca de blocos e kits', 'df-paginas-blocos', 'DF-PAG-BLOCOS',
       'Blocos reutilizáveis e kits de página.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Blocos e kits</span><h2>Biblioteca de blocos e kits</h2><p>Acelere a redação com fragmentos HTML padronizados e kits compostos.</p></section><section class="doc-section"><h2>Kits de página</h2><div class="resource-list resource-list--large"><article class="resource-item"><span class="number-badge">1</span><span><strong>Kit lista</strong><small>Base para páginas de consulta e listagem.</small></span><span class="resource-item__meta">kit-lista</span></article><article class="resource-item"><span class="number-badge">2</span><span><strong>Kit incluir</strong><small>Formulário de inclusão com validações.</small></span><span class="resource-item__meta">kit-incluir</span></article><article class="resource-item"><span class="number-badge">3</span><span><strong>Kit editar</strong><small>Edição com campos e passo a passo.</small></span><span class="resource-item__meta">kit-editar</span></article><article class="resource-item"><span class="number-badge">4</span><span><strong>Kit índice</strong><small>Página pai com guias disponíveis.</small></span><span class="resource-item__meta">kit-indice</span></article><article class="resource-item"><span class="number-badge">5</span><span><strong>Kit menu</strong><small>Pasta de navegação para subpáginas.</small></span><span class="resource-item__meta">kit-menu</span></article></div></section><section class="doc-section"><h2>Categorias de blocos</h2><div class="content-grid content-grid--3"><article class="topic-card"><span class="topic-card__icon">§</span><h3>Estrutura</h3><p>doc-intro, doc-section, objective-card.</p></article><article class="topic-card"><span class="topic-card__icon">✓</span><h3>Listas</h3><p>checklist, steps, resource-list.</p></article><article class="topic-card"><span class="topic-card__icon">⊞</span><h3>Layout</h3><p>content-grid, annotation-grid, topic-card.</p></article></div></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAG-EDITAR">DF-PAG-EDITAR</span></p></div>
$html$,
       'PUBLICADO', 5, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-paginas-blocos');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000226', 'a0000000-0000-4000-8000-000000000112', 'a0000000-0000-4000-8000-000000000220',
       'Hierarquia e subpáginas', 'df-paginas-hierarquia', 'DF-PAG-HIERARQUIA',
       'Árvore de páginas, parentId e tipos pasta/menu.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Hierarquia</span><h2>Hierarquia e subpáginas</h2><p>Organize o manual com páginas pai, subpáginas e ordenação.</p></section><section class="doc-section"><h2>parentId e ordem</h2><p>Defina <code>parentId</code> para vincular subpáginas. Use <code>ordem</code> para sequência na navegação. Arraste na árvore para reorganizar.</p></section><section class="doc-section"><h2>Tipos estruturais</h2><ul class="checklist"><li>Índice (kit-indice): lista guias filhos com resource-list.</li><li>Menu/pasta (kit-menu): agrupa seções sem conteúdo longo.</li><li>Subpágina: conteúdo detalhado vinculado ao pai.</li><li>Índice automático: gerado na publicação a partir da árvore.</li></ul></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-ESTRUTURA">DF-ESTRUTURA</span><span data-codigo-tela="DF-PAG-CRIAR">DF-PAG-CRIAR</span></p></div>
$html$,
       'PUBLICADO', 6, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-paginas-hierarquia');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000230', 'a0000000-0000-4000-8000-000000000113', NULL,
       'Fluxo de revisão', 'df-revisao', 'DF-REVISAO',
       'Workflow RASCUNHO até PUBLICADO na Central de revisão.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Revisão</span><h2>Fluxo de revisão</h2><p>Workflow editorial do DocFlow com rastreabilidade.</p></section><ol class="flow-strip"><li><strong>RASCUNHO</strong><span>Autor edita livremente.</span></li><li><strong>EM_REVISAO</strong><span>Central de revisão analisa.</span></li><li><strong>APROVADO</strong><span>Pronto para publicação.</span></li><li><strong>PUBLICADO</strong><span>Elegível ao pacote.</span></li><li><strong>ARQUIVADO</strong><span>Fora do manual ativo.</span></li></ol><section class="doc-section"><h2>Guias disponíveis</h2><div class="resource-list resource-list--large"><article class="resource-item"><span class="number-badge">1</span><span><strong>Enviar para revisão</strong><small>Checklist e envio da fila editorial.</small></span><span class="resource-item__meta">DF-REV-ENVIAR</span></article><article class="resource-item"><span class="number-badge">2</span><span><strong>Aprovar e devolver</strong><small>Decisões na Central de revisão.</small></span><span class="resource-item__meta">DF-REV-APROVAR</span></article><article class="resource-item"><span class="number-badge">3</span><span><strong>Qualidade editorial</strong><small>Critérios e boas práticas.</small></span><span class="resource-item__meta">DF-REV-QUALIDADE</span></article></div></section>
$html$,
       'PUBLICADO', 0, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-revisao');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000231', 'a0000000-0000-4000-8000-000000000113', 'a0000000-0000-4000-8000-000000000230',
       'Enviar para revisão', 'df-revisao-enviar', 'DF-REV-ENVIAR',
       'Envio para a fila com checklist de qualidade.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Revisão</span><h2>Enviar para revisão</h2><p>Transição de RASCUNHO para EM_REVISAO na Central de revisão.</p></section><section class="doc-section"><h2>Pré-requisitos</h2><ul class="checklist"><li>Checklist sem erros impeditivos.</li><li>Título, código de tela e resumo preenchidos.</li><li>Placeholders e imagens de exemplo substituídos.</li><li>Links internos válidos.</li></ul></section><div class="steps"><h2>Passo a passo</h2><ol><li>Abra a página no editor.</li><li>Revise o painel de qualidade à direita.</li><li>Clique em Enviar para revisão.</li><li>Confirme — a página entra na fila central.</li></ol></div><div class="result-card"><strong>Resultado esperado</strong><p>Status EM_REVISAO; revisores notificados na Central de revisão.</p></div><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-REV-APROVAR">DF-REV-APROVAR</span><span data-codigo-tela="DF-REV-QUALIDADE">DF-REV-QUALIDADE</span></p></div>
$html$,
       'PUBLICADO', 1, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-revisao-enviar');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000232', 'a0000000-0000-4000-8000-000000000113', 'a0000000-0000-4000-8000-000000000230',
       'Aprovar e devolver', 'df-revisao-aprovar', 'DF-REV-APROVAR',
       'Decisões na Central de revisão.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Central de revisão</span><h2>Aprovar e devolver</h2><p>Análise, comparação de versões e decisão editorial.</p></section><div class="steps"><h2>Passo a passo</h2><ol><li>Acesse DocFlow → Central de revisão.</li><li>Assuma a análise da página na fila.</li><li>Compare versão atual com revisões anteriores.</li><li>Aprove (APROVADO) ou devolva com observações (RASCUNHO).</li></ol></div><section class="doc-section"><h2>Registro de decisão</h2><p>Toda ação fica no histórico com autor, data e comentário — essencial para auditoria editorial.</p></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PUB-GERAR">DF-PUB-GERAR</span><span data-codigo-tela="DF-REV-QUALIDADE">DF-REV-QUALIDADE</span></p></div>
$html$,
       'PUBLICADO', 2, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-revisao-aprovar');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000233', 'a0000000-0000-4000-8000-000000000113', 'a0000000-0000-4000-8000-000000000230',
       'Qualidade editorial', 'df-revisao-qualidade', 'DF-REV-QUALIDADE',
       'Critérios e checklist editorial.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Qualidade</span><h2>Qualidade editorial</h2><p>Critérios para conteúdo pronto para publicação.</p></section><section class="doc-section"><h2>Checklist de qualidade</h2><ul class="checklist"><li>Resumo objetivo e sem jargão desnecessário.</li><li>Seções com doc-intro e objective-card quando aplicável.</li><li>Capturas sem dados sensíveis.</li><li>Código de tela único e estável.</li><li>Links relacionados com data-codigo-tela.</li><li>Linguagem em português (Brasil), alinhada à UI.</li></ul></section><section class="doc-section"><h2>Boas práticas</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Objetivo primeiro</h3><p>O leitor entende o propósito antes dos passos.</p></article><article class="rule-card"><h3>Resultado esperado</h3><p>Sempre informe o que muda após a ação.</p></article></div></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAG-EDITAR">DF-PAG-EDITAR</span><span data-codigo-tela="DF-REV-ENVIAR">DF-REV-ENVIAR</span></p></div>
$html$,
       'PUBLICADO', 3, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-revisao-qualidade');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000240', 'a0000000-0000-4000-8000-000000000114', NULL,
       'Publicações', 'df-publicacoes', 'DF-PUB',
       'Índice sobre pacotes, preview e versionamento.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Publicações</span><h2>Publicações</h2><p>Geração e distribuição de pacotes do manual.</p></section><section class="doc-section"><h2>Guias disponíveis</h2><div class="resource-list resource-list--large"><article class="resource-item"><span class="number-badge">1</span><span><strong>Gerar pacote</strong><small>ZIP/PDF e diagnóstico de elegibilidade.</small></span><span class="resource-item__meta">DF-PUB-GERAR</span></article><article class="resource-item"><span class="number-badge">2</span><span><strong>Preview e downloads</strong><small>Token temporário e arquivos.</small></span><span class="resource-item__meta">DF-PUB-PREVIEW</span></article><article class="resource-item"><span class="number-badge">3</span><span><strong>Versionamento</strong><small>Versões por cliente.</small></span><span class="resource-item__meta">DF-PUB-VERSAO</span></article></div></section>
$html$,
       'PUBLICADO', 0, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-publicacoes');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000241', 'a0000000-0000-4000-8000-000000000114', 'a0000000-0000-4000-8000-000000000240',
       'Gerar pacote', 'df-publicacoes-gerar', 'DF-PUB-GERAR',
       'Geração de ZIP/PDF com diagnóstico.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Pacote</span><h2>Gerar pacote</h2><p>Montagem do ZIP/PDF com páginas PUBLICADO vinculadas ao cliente.</p></section><div class="steps"><h2>Passo a passo</h2><ol><li>Em Publicações, clique em Gerar pacote.</li><li>Selecione o Cliente destinatário.</li><li>Revise o diagnóstico: elegíveis, avisos e erros.</li><li>Corrija impedimentos e inicie a geração.</li><li>Acompanhe status até CONCLUÍDA.</li></ol></div><div class="warning"><strong>Impedimentos comuns</strong><p>Página não PUBLICADO, módulo não vinculado ao cliente ou erro de validação no ZIP.</p></div><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PUB-PREVIEW">DF-PUB-PREVIEW</span><span data-codigo-tela="DF-REF-REGRA">DF-REF-REGRA</span></p></div>
$html$,
       'PUBLICADO', 1, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-publicacoes-gerar');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000242', 'a0000000-0000-4000-8000-000000000114', 'a0000000-0000-4000-8000-000000000240',
       'Preview e downloads', 'df-publicacoes-preview', 'DF-PUB-PREVIEW',
       'Preview token e arquivos do pacote.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Distribuição</span><h2>Preview e downloads</h2><p>Preview token e download de artefatos gerados.</p></section><section class="doc-section"><h2>Preview token</h2><p>Link temporário para stakeholders visualizarem o manual antes da distribuição formal. Configure validade e cliente no DocFlow.</p></section><section class="doc-section"><h2>Downloads</h2><p>Após geração concluída, baixe ZIP (HTML estático + assets) ou PDF conforme configuração do pacote.</p></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PUB-VERSAO">DF-PUB-VERSAO</span></p></div>
$html$,
       'PUBLICADO', 2, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-publicacoes-preview');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000243', 'a0000000-0000-4000-8000-000000000114', 'a0000000-0000-4000-8000-000000000240',
       'Versionamento', 'df-publicacoes-versao', 'DF-PUB-VERSAO',
       'Versões e snapshots por cliente.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Versões</span><h2>Versionamento</h2><p>Cada publicação incrementa a versão por cliente (constraint única cliente+versão).</p></section><section class="doc-section"><h2>Regras</h2><ul class="checklist"><li>Nova geração cria versão N+1 para o mesmo cliente.</li><li>Snapshot congela árvore de páginas no momento da geração.</li><li>Reprocessar disponível quando geração falha.</li><li>Changelog registra alterações entre versões.</li></ul></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PUB-GERAR">DF-PUB-GERAR</span><span data-codigo-tela="DF-REF-DOSSIE">DF-REF-DOSSIE</span></p></div>
$html$,
       'PUBLICADO', 3, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-publicacoes-versao');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000250', 'a0000000-0000-4000-8000-000000000115', NULL,
       'Catálogo de modelos de exemplo', 'df-referencia', 'DF-REF',
       'Vitrine dos templates do sistema para novos projetos.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Referência</span><h2>Catálogo de modelos de exemplo</h2><p>Páginas que demonstram templates do sistema — copie a estrutura para novos projetos.</p></section><section class="doc-section"><h2>Guias disponíveis</h2><div class="resource-list resource-list--large"><article class="resource-item"><span class="number-badge">1</span><span><strong>Perguntas frequentes</strong><small>FAQ operacional do DocFlow.</small></span><span class="resource-item__meta">DF-REF-FAQ</span></article><article class="resource-item"><span class="number-badge">2</span><span><strong>Solução de problemas</strong><small>Erros comuns e correções.</small></span><span class="resource-item__meta">DF-REF-TROUBLE</span></article><article class="resource-item"><span class="number-badge">3</span><span><strong>Dicionário de campos</strong><small>Campos do formulário de página.</small></span><span class="resource-item__meta">DF-REF-DICIONARIO</span></article><article class="resource-item"><span class="number-badge">4</span><span><strong>Guia de relatório</strong><small>Métricas do dashboard DocFlow.</small></span><span class="resource-item__meta">DF-REF-RELATORIO</span></article><article class="resource-item"><span class="number-badge">5</span><span><strong>Laboratório de filtros</strong><small>Exemplo com filtros da listagem.</small></span><span class="resource-item__meta">DF-REF-FILTROS</span></article><article class="resource-item"><span class="number-badge">6</span><span><strong>Painel de métricas</strong><small>Saúde editorial do manual.</small></span><span class="resource-item__meta">DF-REF-METRICAS</span></article><article class="resource-item"><span class="number-badge">7</span><span><strong>Dossiê de decisão</strong><small>Go/no-go de publicação.</small></span><span class="resource-item__meta">DF-REF-DOSSIE</span></article><article class="resource-item"><span class="number-badge">8</span><span><strong>Especificação de regra</strong><small>Elegibilidade no pacote.</small></span><span class="resource-item__meta">DF-REF-REGRA</span></article><article class="resource-item"><span class="number-badge">9</span><span><strong>Catálogo de parâmetros</strong><small>Parâmetros de publicação.</small></span><span class="resource-item__meta">DF-REF-PARAM</span></article><article class="resource-item"><span class="number-badge">10</span><span><strong>Cadastro (modelo)</strong><small>Template CADASTRO preenchido.</small></span><span class="resource-item__meta">DF-REF-CADASTRO</span></article><article class="resource-item"><span class="number-badge">11</span><span><strong>Consulta (modelo)</strong><small>Template CONSULTA preenchido.</small></span><span class="resource-item__meta">DF-REF-CONSULTA</span></article><article class="resource-item"><span class="number-badge">12</span><span><strong>Passo a passo (modelo)</strong><small>Template PASSO_A_PASSO.</small></span><span class="resource-item__meta">DF-REF-PASSO</span></article><article class="resource-item"><span class="number-badge">13</span><span><strong>Funcionalidade (modelo)</strong><small>Template FUNCIONALIDADE.</small></span><span class="resource-item__meta">DF-REF-FUNC</span></article></div></section><div class="callout"><strong>Como usar</strong><p>Estas páginas são exemplos reais em uso neste manual. Duplique a estrutura HTML ao documentar outro produto.</p></div>
$html$,
       'PUBLICADO', 0, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-referencia');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000251', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Perguntas frequentes', 'df-ref-faq', 'DF-REF-FAQ',
       'FAQ sobre operação do DocFlow.',
       $html$
<section class="doc-intro"><span class="doc-kicker">FAQ</span><h2>Perguntas frequentes</h2><p>Respostas rápidas sobre o DocFlow no Softon Portal.</p></section><div class="faq-list"><article class="faq-item"><h3>Qual a ordem correta dos cadastros?</h3><p>Cliente, projeto, módulo e página. Depois revise, aprove, publique e gere o pacote.</p></article><article class="faq-item"><h3>Por que não consigo enviar para revisão?</h3><p>Verifique o checklist: conteúdo mínimo, placeholders, imagens, links e campos obrigatórios.</p></article><article class="faq-item"><h3>Por que uma página não aparece no pacote?</h3><p>Precisa estar PUBLICADO e vinculada ao cliente via projeto, módulo ou vínculo direto.</p></article><article class="faq-item"><h3>O que fazer quando a publicação falha?</h3><p>Abra o diagnóstico, corrija o impedimento e use Reprocessar.</p></article><article class="faq-item"><h3>Qual diferença entre Modelo e Kit?</h3><p>Modelo é template de página inteira; Kit é conjunto de blocos para tipos lista/incluir/editar/índice/menu.</p></article></div><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-START">DF-START</span><span data-codigo-tela="DF-PUB-GERAR">DF-PUB-GERAR</span></p></div>
$html$,
       'PUBLICADO', 1, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-faq');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000252', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Solução de problemas', 'df-ref-trouble', 'DF-REF-TROUBLE',
       'Diagnóstico de erros comuns.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Suporte</span><h2>Solução de problemas</h2><p>Diagnóstico de situações recorrentes no DocFlow.</p></section><section class="doc-section"><h2>Problemas e soluções</h2><div class="table-wrap"><table><thead><tr><th>Sintoma</th><th>Causa provável</th><th>Ação</th></tr></thead><tbody><tr><td>Envio para revisão bloqueado</td><td>Checklist com erro impeditivo</td><td>Corrija itens no editor</td></tr><tr><td>Página ausente no pacote</td><td>Status não PUBLICADO ou sem vínculo</td><td>Publique e vincule ao cliente</td></tr><tr><td>Publicação em FALHA</td><td>Validação ZIP ou asset inválido</td><td>Diagnóstico + Reprocessar</td></tr><tr><td>Slug duplicado</td><td>Slug já existe no catálogo</td><td>Escolha outro slug único</td></tr></tbody></table></div></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-REF-FAQ">DF-REF-FAQ</span></p></div>
$html$,
       'PUBLICADO', 2, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-trouble');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000253', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Dicionário de campos', 'df-ref-dicionario', 'DF-REF-DICIONARIO',
       'Campos titulo, codigoTela, resumo, conteudoHtml, parentId, status.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Campos</span><h2>Dicionário de campos</h2><p>Referência dos campos do formulário de Página no DocFlow.</p></section><div class="table-wrap"><table class="dictionary-table"><thead><tr><th>Campo</th><th>Descrição</th><th>Obrigatório</th></tr></thead><tbody><tr><td><strong>titulo</strong></td><td>Título exibido na navegação e busca</td><td><span class="status-badge status-badge--required">Sim</span></td></tr><tr><td><strong>codigoTela</strong></td><td>Identificador único estável (ex.: DF-PAG-LISTA)</td><td><span class="status-badge status-badge--required">Sim</span></td></tr><tr><td><strong>resumo</strong></td><td>Texto curto para listagens e SEO interno</td><td><span class="status-badge status-badge--sim">Recomendado</span></td></tr><tr><td><strong>conteudoHtml</strong></td><td>Corpo HTML com classes do design system</td><td><span class="status-badge status-badge--required">Sim</span></td></tr><tr><td><strong>parentId</strong></td><td>UUID da página pai na hierarquia</td><td><span class="status-badge">Opcional</span></td></tr><tr><td><strong>status</strong></td><td>RASCUNHO, EM_REVISAO, APROVADO, PUBLICADO, ARQUIVADO</td><td><span class="status-badge status-badge--required">Sim</span></td></tr></tbody></table></div><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAG-EDITAR">DF-PAG-EDITAR</span></p></div>
$html$,
       'PUBLICADO', 3, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-dicionario');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000254', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Guia de relatório', 'df-ref-relatorio', 'DF-REF-RELATORIO',
       'Métricas do dashboard DocFlow.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Relatórios</span><h2>Guia de relatório</h2><p>Interpretação das métricas do dashboard DocFlow.</p></section><div class="objective-card"><p><strong>Objetivo</strong></p><p>Acompanhar saúde editorial: pendências, revisões e sucesso de publicações.</p></div><section class="doc-section"><h2>Métricas do dashboard</h2><div class="annotation-grid annotation-grid--3"><article class="annotation-card"><h3>Estrutura</h3><p>Total de clientes, projetos, módulos e páginas.</p></article><article class="annotation-card"><h3>Operacional</h3><p>Páginas pendentes, em revisão, publicações gerando.</p></article><article class="annotation-card"><h3>Qualidade</h3><p>Páginas sem resumo, desatualizadas, taxa de sucesso.</p></article></div></section><section class="doc-section"><h2>Indicadores-chave</h2><ul class="checklist"><li><strong>paginasPendentes</strong> — rascunhos aguardando envio.</li><li><strong>paginasEmRevisao</strong> — fila da Central de revisão.</li><li><strong>publicacoesComErro</strong> — pacotes que exigem reprocessamento.</li><li><strong>clientesSemPublicacao</strong> — clientes sem pacote gerado.</li></ul></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-REF-METRICAS">DF-REF-METRICAS</span></p></div>
$html$,
       'PUBLICADO', 4, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-relatorio');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000255', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Laboratório de filtros', 'df-ref-filtros', 'DF-REF-FILTROS',
       'Exemplo LAB_FILTROS com filtros de páginas.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Filtros</span><h2>Laboratório de filtros</h2><p>Exemplo aplicado aos filtros da listagem de páginas DocFlow.</p></section><section class="doc-section"><h2>Receita de critérios</h2><div class="table-wrap"><table class="criteria-table"><thead><tr><th>Filtro</th><th>Quando usar</th><th>Combina com</th></tr></thead><tbody><tr><td>Busca</td><td>Localizar por título ou código</td><td>Status, módulo</td></tr><tr><td>Status PUBLICADO</td><td>Auditar conteúdo no pacote</td><td>Projeto Manual DocFlow</td></tr><tr><td>Módulo Referência</td><td>Revisar templates de exemplo</td><td>Busca DF-REF</td></tr></tbody></table></div></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAG-LISTA">DF-PAG-LISTA</span></p></div>
$html$,
       'PUBLICADO', 5, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-filtros');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000256', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Painel de métricas', 'df-ref-metricas', 'DF-REF-METRICAS',
       'Exemplo PAINEL_METRICAS editorial.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Métricas</span><h2>Painel de métricas</h2><p>Saúde editorial do manual DocFlow em tempo operacional.</p></section><section class="doc-section"><h2>Indicadores</h2><div class="metric-board"><article class="metric-card"><span class="metric-card__label">Páginas PUBLICADO</span><strong class="metric-card__value">37</strong><p>Elegíveis ao pacote deste manual.</p></article><article class="metric-card"><span class="metric-card__label">Em revisão</span><strong class="metric-card__value">—</strong><p>Fila atual da Central de revisão.</p></article><article class="metric-card"><span class="metric-card__label">Módulos</span><strong class="metric-card__value">6</strong><p>Seções do Manual DocFlow.</p></article><article class="metric-card"><span class="metric-card__label">Taxa sucesso pub.</span><strong class="metric-card__value">—</strong><p>Publicações concluídas vs. falhas.</p></article></div></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-REF-RELATORIO">DF-REF-RELATORIO</span></p></div>
$html$,
       'PUBLICADO', 6, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-metricas');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000257', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Dossiê de decisão', 'df-ref-dossie', 'DF-REF-DOSSIE',
       'Exemplo DOSSIE_DECISAO para publicação.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Decisão</span><h2>Dossiê de decisão</h2><p>Matriz go/no-go antes de gerar uma nova Publicação.</p></section><section class="doc-section"><h2>Evidências</h2><ul class="rank-list"><li>Diagnóstico sem erros impeditivos.</li><li>Páginas críticas PUBLICADO e revisadas.</li><li>Stakeholders alinhados ao escopo da versão.</li></ul></section><section class="doc-section"><h2>Parecer</h2><div class="table-wrap"><table><thead><tr><th>Decisão</th><th>Quando</th></tr></thead><tbody><tr><td><strong>Go</strong></td><td>Diagnóstico limpo; versão pronta</td></tr><tr><td><strong>No-go</strong></td><td>Erros ou conteúdo crítico pendente</td></tr><tr><td><strong>Go parcial</strong></td><td>Publicar subset documentado no changelog</td></tr></tbody></table></div></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PUB-GERAR">DF-PUB-GERAR</span></p></div>
$html$,
       'PUBLICADO', 7, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-dossie');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000258', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Especificação de regra', 'df-ref-regra', 'DF-REF-REGRA',
       'Regras de elegibilidade no pacote.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Regras</span><h2>Especificação de regra</h2><p>Regras de elegibilidade de páginas no pacote de publicação.</p></section><section class="doc-section"><div class="condition-stack"><article class="condition-block"><span class="condition-block__label">SE</span><h3>Página PUBLICADO</h3><p>E status ativo e vinculada ao cliente.</p></article><article class="condition-block condition-block--then"><span class="condition-block__label">ENTÃO</span><h3>Incluir no snapshot</h3><p>Entra na árvore do pacote gerado.</p></article></div></section><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PUB-VERSAO">DF-PUB-VERSAO</span></p></div>
$html$,
       'PUBLICADO', 8, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-regra');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000259', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Catálogo de parâmetros', 'df-ref-parametros', 'DF-REF-PARAM',
       'Parâmetros de publicação DocFlow.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Parâmetros</span><h2>Catálogo de parâmetros</h2><p>Parâmetros que influenciam geração e distribuição do pacote.</p></section><div class="table-wrap"><table class="dictionary-table"><thead><tr><th>Parâmetro</th><th>Efeito</th></tr></thead><tbody><tr><td><strong>cliente_id</strong></td><td>Destinatário e tema do pacote</td></tr><tr><td><strong>versao</strong></td><td>Incremento automático por cliente</td></tr><tr><td><strong>arvore_paginas</strong></td><td>Snapshot JSON da hierarquia</td></tr><tr><td><strong>relatorio_validacao</strong></td><td>Resultado da validação do ZIP</td></tr></tbody></table></div><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PUB-PREVIEW">DF-PUB-PREVIEW</span></p></div>
$html$,
       'PUBLICADO', 9, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-parametros');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000260', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Cadastro ou edição (modelo)', 'df-ref-cadastro', 'DF-REF-CADASTRO',
       'Template CADASTRO preenchido.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Cadastro</span><h2>Cadastro ou edição (modelo)</h2><p>Exemplo do template CADASTRO aplicado ao formulário de Página DocFlow.</p></section><div class="objective-card"><p><strong>Objetivo</strong></p><p>Demonstrar estrutura de cadastro com captura, campos e validações.</p></div><section class="doc-section"><h2>Visão da tela</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Formulário de nova página</strong><span>Captura do DocFlow com módulo, título e editor.</span></div><figcaption>Tela de cadastro de página no Softon Portal.</figcaption></figure></section><section class="doc-section"><h2>Campos do formulário</h2><div class="annotation-grid annotation-grid--2"><article class="annotation-card"><h3>Título</h3><p>Nome exibido na navegação.</p></article><article class="annotation-card"><h3>Código de tela</h3><p>Identificador único DF-XXX.</p></article><article class="annotation-card"><h3>Módulo</h3><p>Área funcional do manual.</p></article><article class="annotation-card"><h3>Conteúdo HTML</h3><p>Corpo com classes do design system.</p></article></div></section><section class="doc-section"><h2>Validações</h2><ul class="checklist"><li><code>codigoTela</code> único em todo o catálogo.</li><li><code>slug</code> único — usado na URL do manual publicado.</li><li>Resumo recomendado para busca e listagens.</li><li>Substituir placeholders do modelo antes da revisão.</li></ul></section><div class="steps"><h2>Passo a passo</h2><ol><li>Acesse DocFlow → Páginas → Nova página.</li><li>Selecione módulo, tipo e modelo CADASTRO.</li><li>Preencha título, slug, código de tela e resumo.</li><li>Edite o HTML nos modos Rico, Código ou Split.</li><li>Salve e envie para revisão quando o checklist estiver verde.</li></ol></div><div class="warning"><strong>Atenção</strong><p>Alterar <code>slug</code> após publicação pode quebrar links externos ao manual.</p></div><div class="result-card"><strong>Resultado esperado</strong><p>Página criada em RASCUNHO, visível na listagem e pronta para o fluxo de revisão.</p></div><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAG-CRIAR">DF-PAG-CRIAR</span><span data-codigo-tela="DF-REF-CONSULTA">DF-REF-CONSULTA</span><span data-codigo-tela="DF-REF-DICIONARIO">DF-REF-DICIONARIO</span></p></div>
$html$,
       'PUBLICADO', 10, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-cadastro');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000261', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Consulta ou listagem (modelo)', 'df-ref-consulta', 'DF-REF-CONSULTA',
       'Template CONSULTA preenchido.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Consulta</span><h2>Consulta ou listagem (modelo)</h2><p>Exemplo CONSULTA com filtros reais da listagem DocFlow.</p></section><div class="objective-card"><p><strong>Objetivo</strong></p><p>Localizar páginas para editar, revisar ou auditar elegibilidade de publicação.</p></div><section class="doc-section"><h2>Visão da listagem</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Listagem de páginas DocFlow</strong><span>Filtros, grade e ações por linha.</span></div></figure></section><section class="doc-section"><h2>Filtros</h2><div class="table-wrap"><table class="criteria-table"><thead><tr><th>Filtro</th><th>Descrição</th></tr></thead><tbody><tr><td>Busca</td><td>Título, slug ou código de tela</td></tr><tr><td>Status</td><td>RASCUNHO, EM_REVISAO, PUBLICADO</td></tr><tr><td>Projeto</td><td>Manual DocFlow ou outro projeto</td></tr><tr><td>Módulo</td><td>Início, Conteúdo, Referência…</td></tr></tbody></table></div></section><section class="doc-section"><h2>Ações por registro</h2><div class="annotation-grid annotation-grid--2"><article class="annotation-card"><h3>Editar</h3><p>Abre o editor da página.</p></article><article class="annotation-card"><h3>Enviar para revisão</h3><p>Disponível em RASCUNHO sem erros impeditivos.</p></article></div></section><div class="result-card"><strong>Resultado esperado</strong><p>Grade com páginas que atendem aos critérios selecionados.</p></div><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PAG-LISTA">DF-PAG-LISTA</span><span data-codigo-tela="DF-REF-FILTROS">DF-REF-FILTROS</span></p></div>
$html$,
       'PUBLICADO', 11, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-consulta');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000262', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Passo a passo visual (modelo)', 'df-ref-passo', 'DF-REF-PASSO',
       'Template PASSO_A_PASSO.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Procedimento</span><h2>Passo a passo visual (modelo)</h2><p>Exemplo PASSO_A_PASSO para publicar uma página no DocFlow.</p></section><div class="objective-card"><p><strong>Objetivo</strong></p><p>Conduzir uma página do rascunho até PUBLICADO com rastreabilidade editorial.</p></div><section class="doc-section doc-section--soft"><h2>Pré-requisitos</h2><ul class="checklist"><li>Perfil Editor ou superior.</li><li>Página vinculada a módulo do cliente destinatário.</li><li>Checklist de qualidade sem erros impeditivos.</li></ul></section><div class="steps"><h2>Passo a passo</h2><ol><li>Edite a página até o checklist ficar verde.</li><li>Clique em <strong>Enviar para revisão</strong>.</li><li>Na Central de revisão, aguarde aprovação do revisor.</li><li>Após APROVADO, publique a página (status PUBLICADO).</li><li>Em Publicações, confirme elegibilidade no diagnóstico.</li></ol></div><figure class="screen-frame"><div class="screen-placeholder"><strong>Captura do fluxo</strong><span>Substitua por imagem real do DocFlow.</span></div><figcaption>Fluxo editorial no Softon Portal.</figcaption></figure><div class="result-card"><strong>Resultado esperado</strong><p>Página PUBLICADO e listada como elegível no pacote do cliente.</p></div><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-REV-ENVIAR">DF-REV-ENVIAR</span><span data-codigo-tela="DF-PUB-GERAR">DF-PUB-GERAR</span></p></div>
$html$,
       'PUBLICADO', 12, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-passo');

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, version, published_at, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000263', 'a0000000-0000-4000-8000-000000000115', 'a0000000-0000-4000-8000-000000000250',
       'Guia de funcionalidade (modelo)', 'df-ref-funcionalidade', 'DF-REF-FUNC',
       'Template FUNCIONALIDADE.',
       $html$
<section class="doc-intro"><span class="doc-kicker">Funcionalidade</span><h2>Guia de funcionalidade (modelo)</h2><p>Exemplo FUNCIONALIDADE para a área de Publicações do DocFlow.</p></section><div class="objective-card"><p><strong>Objetivo</strong></p><p>Entender geração de pacote ZIP/PDF para distribuição do manual ao cliente.</p></div><section class="doc-section"><h2>Visão da funcionalidade</h2><figure class="screen-frame"><div class="screen-placeholder"><strong>Tela de Publicações</strong><span>Diagnóstico, geração e downloads.</span></div><figcaption>Área Publicações no Softon Portal DocFlow.</figcaption></figure></section><section class="doc-section"><h2>Elementos principais</h2><div class="annotation-grid annotation-grid--3"><article class="annotation-card"><h3>Diagnóstico</h3><p>Páginas elegíveis, avisos e erros antes da geração.</p></article><article class="annotation-card"><h3>Geração</h3><p>Processamento assíncrono do pacote versionado.</p></article><article class="annotation-card"><h3>Download</h3><p>ZIP (HTML estático) e PDF após conclusão.</p></article></div></section><section class="doc-section"><h2>Regras e boas práticas</h2><div class="content-grid content-grid--2"><article class="rule-card"><h3>Regras</h3><ul class="checklist"><li>Somente páginas PUBLICADO entram no snapshot.</li><li>Vínculo cliente via projeto, módulo ou página direta.</li><li>Versão única por cliente incrementa automaticamente.</li></ul></article><article class="rule-card"><h3>Boas práticas</h3><ul class="checklist"><li>Revise o diagnóstico antes de gerar.</li><li>Documente mudanças no changelog da versão.</li><li>Use preview token para validação com stakeholders.</li></ul></article></div></section><div class="result-card"><strong>Resultado esperado</strong><p>Pacote CONCLUÍDO disponível para download e distribuição.</p></div><div class="related-links"><p><strong>Ver também</strong><span data-codigo-tela="DF-PUB-GERAR">DF-PUB-GERAR</span><span data-codigo-tela="DF-PUB-PREVIEW">DF-PUB-PREVIEW</span><span data-codigo-tela="DF-REF-DOSSIE">DF-REF-DOSSIE</span></p></div>
$html$,
       'PUBLICADO', 13, TRUE, 0, now(), 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_pagina WHERE slug = 'df-ref-funcionalidade');
