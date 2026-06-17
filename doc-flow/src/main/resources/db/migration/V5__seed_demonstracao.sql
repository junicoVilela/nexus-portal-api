-- =============================================================================
-- SEED DE DEMONSTRAÇÃO
-- Dados realistas para desenvolvimento, testes e apresentações.
-- Credenciais padrão: admin / admin  |  editor / editor  |  revisor / revisor
-- =============================================================================

-- =============================================================================
-- PROJETOS
-- =============================================================================

INSERT INTO tb_projeto (id, nome, slug, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  (
    '00000000-0000-0000-0000-000000000900',
    'Plataforma de Manuais',
    'plataforma-de-manuais',
    'Documentação completa da plataforma: administração, cadastros, fluxo de documentos e publicações.',
    true, now(), now(), 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000901',
    'Portal do Colaborador',
    'portal-do-colaborador',
    'Guias de onboarding, benefícios e treinamentos para novos colaboradores.',
    true, now(), now(), 'seed', 'seed'
  )
ON CONFLICT (slug) DO UPDATE SET
  nome       = EXCLUDED.nome,
  descricao  = EXCLUDED.descricao,
  ativo      = EXCLUDED.ativo,
  updated_at = now();

-- =============================================================================
-- MÓDULOS — Plataforma de Manuais
-- =============================================================================

INSERT INTO tb_modulo (id, projeto_id, nome, slug, descricao, ordem, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000900',
   'Administração', 'administracao', 'Configurações gerais, usuários, clientes e permissões do sistema.',
   10, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000900',
   'Cadastros', 'cadastros', 'Cadastros operacionais utilizados pelo sistema.',
   20, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000203', '00000000-0000-0000-0000-000000000900',
   'Documentos', 'documentos', 'Fluxos de documentos, revisão, aprovação e consulta.',
   30, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000204', '00000000-0000-0000-0000-000000000900',
   'Publicações', 'publicacoes', 'Geração, versionamento e download dos pacotes de manual.',
   40, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000205', '00000000-0000-0000-0000-000000000900',
   'Relatórios', 'relatorios', 'Dashboards e relatórios analíticos do sistema.',
   50, true, now(), now(), 'seed', 'seed')
ON CONFLICT (projeto_id, slug) DO UPDATE SET
  nome       = EXCLUDED.nome,
  descricao  = EXCLUDED.descricao,
  ordem      = EXCLUDED.ordem,
  ativo      = EXCLUDED.ativo,
  updated_at = now();

-- Módulos — Portal do Colaborador
INSERT INTO tb_modulo (id, projeto_id, nome, slug, descricao, ordem, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0000-000000000211', '00000000-0000-0000-0000-000000000901',
   'Onboarding', 'onboarding', 'Primeiros passos, acesso e configuração inicial para novos colaboradores.',
   10, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000212', '00000000-0000-0000-0000-000000000901',
   'Benefícios', 'beneficios', 'Guia de benefícios: plano de saúde, vale-refeição e auxílios.',
   20, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000213', '00000000-0000-0000-0000-000000000901',
   'Treinamentos', 'treinamentos', 'Trilhas de aprendizado obrigatórias e eletivas.',
   30, true, now(), now(), 'seed', 'seed')
ON CONFLICT (projeto_id, slug) DO UPDATE SET
  nome       = EXCLUDED.nome,
  descricao  = EXCLUDED.descricao,
  ordem      = EXCLUDED.ordem,
  ativo      = EXCLUDED.ativo,
  updated_at = now();

-- =============================================================================
-- PÁGINAS RAIZ — Plataforma de Manuais
-- =============================================================================

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, published_at, created_at, updated_at, created_by, updated_by
) VALUES
  (
    '00000000-0000-0000-0000-000000000301',
    (SELECT id FROM tb_modulo WHERE slug = 'administracao'), NULL,
    'Visão geral do painel', 'visao-geral-do-painel', 'DASHBOARD_HOME',
    'Apresenta indicadores iniciais e atalhos para os fluxos mais usados.',
    '<h1>Visão geral do painel</h1><p>O painel principal concentra os indicadores mais importantes do sistema: clientes ativos, páginas publicadas, publicações recentes e pendências de revisão.</p><h2>Como usar</h2><ol><li>Confira os indicadores no topo para ter uma visão rápida da saúde do sistema.</li><li>Use os atalhos para navegar diretamente para clientes, publicações ou páginas em revisão.</li><li>Revise pendências antes de iniciar uma nova publicação.</li></ol>',
    'PUBLICADO', 10, true, now() - interval '10 days', now() - interval '15 days', now() - interval '10 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000302',
    (SELECT id FROM tb_modulo WHERE slug = 'administracao'), NULL,
    'Gestão de usuários', 'gestao-de-usuarios', 'USUARIOS_LISTA',
    'Criação, edição e controle de acesso dos usuários do sistema.',
    '<h1>Gestão de usuários</h1><p>Acesse o menu <strong>Usuários</strong> para visualizar todos os usuários cadastrados, seus papéis e status.</p><h2>Papéis disponíveis</h2><ul><li><strong>ADMIN</strong>: acesso total, incluindo configurações e exclusões.</li><li><strong>EDITOR</strong>: pode criar e editar páginas, mas não gerenciar usuários.</li></ul><h2>Boas práticas</h2><p>Crie um usuário por pessoa. Nunca compartilhe credenciais entre colaboradores.</p>',
    'PUBLICADO', 20, true, now() - interval '8 days', now() - interval '15 days', now() - interval '8 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000310',
    (SELECT id FROM tb_modulo WHERE slug = 'cadastros'), NULL,
    'Cadastro de clientes', 'cadastro-de-clientes', 'CLIENTES_LISTA',
    'Demonstra criação, edição e ativação de clientes.',
    '<h1>Cadastro de clientes</h1><p>Cadastre o cliente com nome e slug únicos. O slug é utilizado nas URLs dos pacotes gerados.</p><h2>Campos obrigatórios</h2><ul><li><strong>Nome</strong>: nome completo ou razão social.</li><li><strong>Slug</strong>: identificador único em letras minúsculas e hífens.</li></ul><h2>Vínculos</h2><p>Após salvar o cliente, defina quais módulos e páginas estarão disponíveis no manual gerado para ele.</p>',
    'PUBLICADO', 10, true, now() - interval '12 days', now() - interval '15 days', now() - interval '12 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000311',
    (SELECT id FROM tb_modulo WHERE slug = 'cadastros'), NULL,
    'Cadastro de módulos', 'cadastro-de-modulos', 'MODULOS_LISTA',
    'Explica como criar módulos e ordenar o conteúdo do manual.',
    '<h1>Cadastro de módulos</h1><p>Módulos agrupam páginas relacionadas e definem a estrutura de navegação do manual estático.</p><h2>Ordenação</h2><p>Use o campo <strong>Ordem</strong> para definir a sequência em que os módulos aparecem no manual. Valores menores aparecem primeiro.</p><h2>Inativação</h2><p>Módulos inativos não entram nas próximas publicações, mas o histórico é preservado.</p>',
    'PUBLICADO', 20, true, now() - interval '12 days', now() - interval '15 days', now() - interval '12 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000312',
    (SELECT id FROM tb_modulo WHERE slug = 'cadastros'), NULL,
    'Cadastro de agência', 'cadastro-de-agencia', 'CAD_AGENCIA',
    'Exemplo de tela operacional comum para clientes do setor financeiro.',
    '<h1>Cadastro de agência</h1><p>Informe código, nome, praça e situação da agência antes de salvar.</p><p>Use os filtros de pesquisa para localizar registros existentes e evitar duplicatas.</p><h2>Campos</h2><ul><li><strong>Código</strong>: número único da agência no sistema.</li><li><strong>Praça</strong>: cidade ou região de atuação.</li><li><strong>Situação</strong>: Ativa ou Inativa.</li></ul>',
    'PUBLICADO', 30, true, now() - interval '7 days', now() - interval '15 days', now() - interval '7 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000320',
    (SELECT id FROM tb_modulo WHERE slug = 'documentos'), NULL,
    'Consulta de documentos', 'consulta-de-documentos', 'DOC_CONSULTA',
    'Explica pesquisa, filtros e abertura de documentos.',
    '<h1>Consulta de documentos</h1><p>A tela de consulta permite localizar documentos por cliente, período, status e número.</p><h2>Filtros disponíveis</h2><ul><li>Cliente</li><li>Período (data inicial e final)</li><li>Status (Pendente, Em revisão, Aprovado)</li><li>Número do documento</li></ul><p>Após localizar o documento, clique em <strong>Abrir</strong> para visualizar detalhes e anexos.</p>',
    'PUBLICADO', 10, true, now() - interval '6 days', now() - interval '15 days', now() - interval '6 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000321',
    (SELECT id FROM tb_modulo WHERE slug = 'documentos'), NULL,
    'Envio de documentos', 'envio-de-documentos', 'DOC_ENVIO',
    'Passo a passo para enviar novos documentos ao sistema.',
    '<h1>Envio de documentos</h1><p>Para enviar um novo documento, acesse <strong>Documentos &gt; Novo</strong> e preencha os campos obrigatórios.</p><h2>Tipos aceitos</h2><ul><li>PDF (máx. 20 MB)</li><li>DOCX (máx. 10 MB)</li><li>Imagens JPG e PNG (máx. 5 MB cada)</li></ul><h2>Atenção</h2><p>Documentos enviados com sucesso entram automaticamente na fila de revisão.</p>',
    'EM_REVISAO', 20, true, NULL, now() - interval '3 days', now() - interval '1 day', 'seed', 'revisor'
  ),
  (
    '00000000-0000-0000-0000-000000000322',
    (SELECT id FROM tb_modulo WHERE slug = 'documentos'), NULL,
    'Aprovação e devolução', 'aprovacao-e-devolucao', 'DOC_APROVACAO',
    'Fluxo completo de aprovação, parecer e devolução de documentos.',
    '<h1>Aprovação e devolução</h1><p>Revisores com papel <strong>EDITOR</strong> ou superior podem aprovar ou devolver documentos.</p><h2>Para aprovar</h2><ol><li>Abra o documento na fila de revisão.</li><li>Confira todos os campos e anexos.</li><li>Clique em <strong>Aprovar</strong> e confirme.</li></ol><h2>Para devolver</h2><p>Clique em <strong>Devolver</strong>, descreva claramente o motivo e confirme. O solicitante receberá notificação por e-mail.</p>',
    'APROVADO', 30, true, NULL, now() - interval '5 days', now() - interval '2 days', 'seed', 'revisor'
  ),
  (
    '00000000-0000-0000-0000-000000000330',
    (SELECT id FROM tb_modulo WHERE slug = 'publicacoes'), NULL,
    'Geração de publicação', 'geracao-de-publicacao', 'PUBLICACOES_GERAR',
    'Demonstra preview, versão semântica e geração do pacote ZIP.',
    '<h1>Geração de publicação</h1><p>Para gerar um novo pacote de manual, acesse <strong>Publicações &gt; Nova publicação</strong>.</p><h2>Passos</h2><ol><li>Selecione o cliente.</li><li>Informe a versão no formato semântico (ex.: 2.1.0).</li><li>Use o preview para confirmar quais páginas entrarão no pacote.</li><li>Clique em <strong>Gerar</strong> e aguarde o processamento em background.</li></ol><h2>Atenção</h2><p>Apenas páginas com status <strong>PUBLICADO</strong> são incluídas no pacote.</p>',
    'PUBLICADO', 10, true, now() - interval '9 days', now() - interval '15 days', now() - interval '9 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000331',
    (SELECT id FROM tb_modulo WHERE slug = 'publicacoes'), NULL,
    'Histórico de publicações', 'historico-de-publicacoes', 'PUBLICACOES_HISTORICO',
    'Mostra status, quantidade de páginas e download de pacotes gerados.',
    '<h1>Histórico de publicações</h1><p>O histórico exibe todas as publicações de um cliente em ordem cronológica decrescente.</p><h2>Status possíveis</h2><ul><li><strong>GERANDO</strong>: processamento em andamento.</li><li><strong>SUCESSO</strong>: pacote disponível para download.</li><li><strong>ERRO</strong>: falha no processamento. Detalhes no campo observação.</li></ul><p>Use o botão <strong>Download</strong> para baixar o pacote ZIP de publicações com sucesso.</p>',
    'PUBLICADO', 20, true, now() - interval '9 days', now() - interval '15 days', now() - interval '9 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000340',
    (SELECT id FROM tb_modulo WHERE slug = 'relatorios'), NULL,
    'Relatório de cobertura por cliente', 'relatorio-cobertura-cliente', 'REL_COBERTURA',
    'Exibe quais módulos e páginas cada cliente tem acesso.',
    '<h1>Relatório de cobertura por cliente</h1><p>Este relatório apresenta, para cada cliente ativo, quais módulos e páginas estão vinculados e prontos para publicação.</p><h2>Como interpretar</h2><ul><li>Clientes com poucos vínculos podem não ter manual completo.</li><li>Páginas em rascunho vinculadas a um cliente indicam conteúdo pendente.</li></ul>',
    'RASCUNHO', 10, true, NULL, now() - interval '1 day', now() - interval '1 day', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000341',
    (SELECT id FROM tb_modulo WHERE slug = 'relatorios'), NULL,
    'Auditoria de alterações', 'auditoria-de-alteracoes', 'REL_AUDITORIA',
    'Registro completo de quem alterou o quê e quando.',
    '<h1>Auditoria de alterações</h1><p>Todas as operações de criação, edição e exclusão são registradas com usuário, data e detalhes da alteração.</p><h2>Filtros</h2><ul><li>Entidade (Página, Publicação, Cliente, etc.)</li><li>Usuário responsável</li><li>Período</li></ul>',
    'RASCUNHO', 20, true, NULL, now() - interval '1 day', now() - interval '1 day', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000350',
    (SELECT id FROM tb_modulo WHERE slug = 'documentos'), NULL,
    'Exportação de relatório PDF', 'exportacao-relatorio-pdf', 'DOC_EXPORT_PDF',
    'Como exportar documentos e relatórios em formato PDF.',
    '<h1>Exportação em PDF</h1><p>Use o botão <strong>Exportar PDF</strong> disponível nas telas de detalhe e relatório para gerar uma cópia em PDF.</p><p>O arquivo é gerado em background e fica disponível para download em poucos segundos.</p>',
    'ARQUIVADO', 99, false, NULL, now() - interval '30 days', now() - interval '20 days', 'seed', 'seed'
  )
ON CONFLICT (slug) DO UPDATE SET
  titulo        = EXCLUDED.titulo,
  codigo_tela   = EXCLUDED.codigo_tela,
  resumo        = EXCLUDED.resumo,
  conteudo_html = EXCLUDED.conteudo_html,
  status        = EXCLUDED.status,
  ordem         = EXCLUDED.ordem,
  ativo         = EXCLUDED.ativo,
  modulo_id     = EXCLUDED.modulo_id,
  parent_id     = EXCLUDED.parent_id,
  published_at  = EXCLUDED.published_at,
  updated_at    = now(),
  updated_by    = EXCLUDED.updated_by;

-- =============================================================================
-- PÁGINAS FILHAS (inseridas após as raiz por dependência de FK)
-- =============================================================================

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, published_at, created_at, updated_at, created_by, updated_by
) VALUES
  (
    '00000000-0000-0000-0000-000000000303',
    (SELECT id FROM tb_modulo WHERE slug = 'administracao'),
    (SELECT id FROM tb_pagina  WHERE slug = 'visao-geral-do-painel'),
    'Configurações gerais', 'configuracoes-gerais', 'CONFIG_GERAL',
    'Parâmetros globais do sistema como nome, logo e fuso horário.',
    '<h1>Configurações gerais</h1><p>Acesse <strong>Administração &gt; Configurações</strong> para personalizar o sistema.</p><h2>Parâmetros disponíveis</h2><ul><li>Nome do sistema</li><li>Logo (PNG/SVG, máx. 512 KB)</li><li>Fuso horário padrão</li><li>E-mail de notificações</li></ul>',
    'PUBLICADO', 10, true, now() - interval '8 days', now() - interval '15 days', now() - interval '8 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000313',
    (SELECT id FROM tb_modulo WHERE slug = 'cadastros'),
    (SELECT id FROM tb_pagina  WHERE slug = 'cadastro-de-clientes'),
    'Vínculos de módulos e páginas', 'vinculos-modulos-paginas', 'CLIENTES_VINCULOS',
    'Como associar módulos e páginas a um cliente.',
    '<h1>Vínculos de módulos e páginas</h1><p>Na tela de edição do cliente, acesse a aba <strong>Vínculos</strong> para configurar quais módulos e páginas farão parte do manual gerado para esse cliente.</p><h2>Boas práticas</h2><p>Vincule apenas as páginas com status <strong>PUBLICADO</strong> para garantir que o pacote seja gerado corretamente.</p>',
    'PUBLICADO', 10, true, now() - interval '7 days', now() - interval '15 days', now() - interval '7 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000323',
    (SELECT id FROM tb_modulo WHERE slug = 'documentos'),
    (SELECT id FROM tb_pagina  WHERE slug = 'consulta-de-documentos'),
    'Revisão de documento', 'revisao-de-documento', 'DOC_REVISAO',
    'Fluxo detalhado de revisão com parecer obrigatório.',
    '<h1>Revisão de documento</h1><p>Ao abrir um documento para revisão, verifique todos os campos e anexos antes de emitir o parecer.</p><h2>Campos do parecer</h2><ul><li><strong>Decisão</strong>: Aprovar ou Devolver.</li><li><strong>Observação</strong>: campo obrigatório ao devolver.</li></ul><p>Documentos aprovados seguem automaticamente para a fila de publicação.</p>',
    'PUBLICADO', 10, true, now() - interval '6 days', now() - interval '15 days', now() - interval '6 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000332',
    (SELECT id FROM tb_modulo WHERE slug = 'publicacoes'),
    (SELECT id FROM tb_pagina  WHERE slug = 'geracao-de-publicacao'),
    'Preview do pacote', 'preview-do-pacote', 'PUBLICACOES_PREVIEW',
    'Como usar o preview antes de gerar o pacote definitivo.',
    '<h1>Preview do pacote</h1><p>Antes de confirmar a geração, utilize o recurso de <strong>preview</strong> para visualizar exatamente quais páginas e módulos entrarão no pacote.</p><h2>O que verificar</h2><ul><li>Número de páginas publicadas por módulo.</li><li>Páginas em rascunho que poderiam ser publicadas antes da geração.</li><li>Ordenação dos módulos e páginas.</li></ul>',
    'PUBLICADO', 10, true, now() - interval '9 days', now() - interval '15 days', now() - interval '9 days', 'seed', 'seed'
  )
ON CONFLICT (slug) DO UPDATE SET
  titulo        = EXCLUDED.titulo,
  codigo_tela   = EXCLUDED.codigo_tela,
  resumo        = EXCLUDED.resumo,
  conteudo_html = EXCLUDED.conteudo_html,
  status        = EXCLUDED.status,
  ordem         = EXCLUDED.ordem,
  ativo         = EXCLUDED.ativo,
  parent_id     = EXCLUDED.parent_id,
  modulo_id     = EXCLUDED.modulo_id,
  published_at  = EXCLUDED.published_at,
  updated_at    = now(),
  updated_by    = EXCLUDED.updated_by;

-- =============================================================================
-- PÁGINAS — Portal do Colaborador
-- =============================================================================

INSERT INTO tb_pagina (
  id, modulo_id, parent_id, titulo, slug, codigo_tela, resumo, conteudo_html,
  status, ordem, ativo, published_at, created_at, updated_at, created_by, updated_by
) VALUES
  (
    '00000000-0000-0000-0000-000000000360',
    (SELECT id FROM tb_modulo WHERE slug = 'onboarding'), NULL,
    'Primeiro acesso ao sistema', 'primeiro-acesso-ao-sistema', 'ONBOARD_ACESSO',
    'Como criar senha, fazer login e configurar autenticação inicial.',
    '<h1>Primeiro acesso ao sistema</h1><p>Ao receber o e-mail de boas-vindas, clique no link para definir sua senha. A senha deve ter no mínimo 8 caracteres, incluindo letras e números.</p><h2>Passos</h2><ol><li>Clique no link recebido por e-mail.</li><li>Defina uma senha forte.</li><li>Faça o primeiro login.</li><li>Preencha seu perfil.</li></ol>',
    'PUBLICADO', 10, true, now() - interval '5 days', now() - interval '10 days', now() - interval '5 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000361',
    (SELECT id FROM tb_modulo WHERE slug = 'beneficios'), NULL,
    'Plano de saúde', 'plano-de-saude', 'BENEF_SAUDE',
    'Cobertura, dependentes e rede credenciada do plano de saúde.',
    '<h1>Plano de saúde</h1><p>A empresa oferece plano de saúde coletivo com cobertura para titular e dependentes legais.</p><h2>Como cadastrar dependentes</h2><ol><li>Acesse RH &gt; Meus Benefícios.</li><li>Clique em <strong>Adicionar dependente</strong>.</li><li>Informe os documentos solicitados.</li></ol>',
    'PUBLICADO', 10, true, now() - interval '5 days', now() - interval '10 days', now() - interval '5 days', 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000362',
    (SELECT id FROM tb_modulo WHERE slug = 'treinamentos'), NULL,
    'Trilha de integração obrigatória', 'trilha-integracao-obrigatoria', 'TRAIN_INTEGRACAO',
    'Módulos obrigatórios que devem ser concluídos nos primeiros 30 dias.',
    '<h1>Trilha de integração obrigatória</h1><p>Nos primeiros 30 dias, todos os colaboradores devem concluir os módulos abaixo:</p><ol><li>Cultura e Valores (2h)</li><li>Política de Segurança da Informação (1h)</li><li>Código de Conduta (1h)</li><li>Sistemas e Ferramentas (3h)</li></ol><p>O prazo de conclusão é monitorado pelo RH. Em caso de dúvidas, contate <strong>treinamentos@empresa.com.br</strong>.</p>',
    'PUBLICADO', 10, true, now() - interval '5 days', now() - interval '10 days', now() - interval '5 days', 'seed', 'seed'
  )
ON CONFLICT (slug) DO UPDATE SET
  titulo        = EXCLUDED.titulo,
  codigo_tela   = EXCLUDED.codigo_tela,
  resumo        = EXCLUDED.resumo,
  conteudo_html = EXCLUDED.conteudo_html,
  status        = EXCLUDED.status,
  ordem         = EXCLUDED.ordem,
  ativo         = EXCLUDED.ativo,
  modulo_id     = EXCLUDED.modulo_id,
  published_at  = EXCLUDED.published_at,
  updated_at    = now(),
  updated_by    = EXCLUDED.updated_by;

-- =============================================================================
-- CLIENTES
-- =============================================================================

INSERT INTO tb_cliente (id, nome, slug, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0000-000000000101', 'BASA — Banco da Amazônia', 'basa',  true,  now() - interval '20 days', now() - interval '20 days', 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000102', 'BRB — Banco de Brasília',  'brb',   true,  now() - interval '20 days', now() - interval '20 days', 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000103', 'BMW Financial Services',   'bmw',   true,  now() - interval '20 days', now() - interval '20 days', 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000104', 'Caixa Econômica Federal',  'caixa', true,  now() - interval '15 days', now() - interval '15 days', 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000105', 'Itaú Unibanco',            'itau',  false, now() - interval '10 days', now() - interval '5 days',  'seed', 'seed')
ON CONFLICT (slug) DO UPDATE SET
  nome       = EXCLUDED.nome,
  ativo      = EXCLUDED.ativo,
  updated_at = now();

-- =============================================================================
-- VÍNCULOS: cliente ↔ projeto
-- =============================================================================

INSERT INTO tb_cliente_projeto (id, cliente_id, projeto_id) VALUES
  ('00000000-0000-0000-0000-000000000a01', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  '00000000-0000-0000-0000-000000000900'),
  ('00000000-0000-0000-0000-000000000a02', (SELECT id FROM tb_cliente WHERE slug = 'brb'),   '00000000-0000-0000-0000-000000000900'),
  ('00000000-0000-0000-0000-000000000a03', (SELECT id FROM tb_cliente WHERE slug = 'bmw'),   '00000000-0000-0000-0000-000000000900'),
  ('00000000-0000-0000-0000-000000000a04', (SELECT id FROM tb_cliente WHERE slug = 'caixa'), '00000000-0000-0000-0000-000000000900'),
  ('00000000-0000-0000-0000-000000000a05', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  '00000000-0000-0000-0000-000000000901'),
  ('00000000-0000-0000-0000-000000000a06', (SELECT id FROM tb_cliente WHERE slug = 'caixa'), '00000000-0000-0000-0000-000000000901')
ON CONFLICT (cliente_id, projeto_id) DO NOTHING;

-- =============================================================================
-- VÍNCULOS: cliente ↔ módulo
-- =============================================================================

INSERT INTO tb_cliente_modulo (id, cliente_id, modulo_id) VALUES
  ('00000000-0000-0000-0000-000000000401', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_modulo WHERE slug = 'administracao')),
  ('00000000-0000-0000-0000-000000000402', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_modulo WHERE slug = 'cadastros')),
  ('00000000-0000-0000-0000-000000000403', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_modulo WHERE slug = 'documentos')),
  ('00000000-0000-0000-0000-000000000404', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_modulo WHERE slug = 'publicacoes')),
  ('00000000-0000-0000-0000-000000000405', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_modulo WHERE slug = 'relatorios')),
  ('00000000-0000-0000-0000-000000000406', (SELECT id FROM tb_cliente WHERE slug = 'brb'),   (SELECT id FROM tb_modulo WHERE slug = 'cadastros')),
  ('00000000-0000-0000-0000-000000000407', (SELECT id FROM tb_cliente WHERE slug = 'brb'),   (SELECT id FROM tb_modulo WHERE slug = 'documentos')),
  ('00000000-0000-0000-0000-000000000408', (SELECT id FROM tb_cliente WHERE slug = 'brb'),   (SELECT id FROM tb_modulo WHERE slug = 'publicacoes')),
  ('00000000-0000-0000-0000-000000000409', (SELECT id FROM tb_cliente WHERE slug = 'bmw'),   (SELECT id FROM tb_modulo WHERE slug = 'documentos')),
  ('00000000-0000-0000-0000-000000000410', (SELECT id FROM tb_cliente WHERE slug = 'caixa'), (SELECT id FROM tb_modulo WHERE slug = 'administracao')),
  ('00000000-0000-0000-0000-000000000411', (SELECT id FROM tb_cliente WHERE slug = 'caixa'), (SELECT id FROM tb_modulo WHERE slug = 'cadastros')),
  ('00000000-0000-0000-0000-000000000412', (SELECT id FROM tb_cliente WHERE slug = 'caixa'), (SELECT id FROM tb_modulo WHERE slug = 'documentos')),
  ('00000000-0000-0000-0000-000000000413', (SELECT id FROM tb_cliente WHERE slug = 'caixa'), (SELECT id FROM tb_modulo WHERE slug = 'publicacoes'))
ON CONFLICT (cliente_id, modulo_id) DO NOTHING;

-- =============================================================================
-- VÍNCULOS: cliente ↔ página
-- =============================================================================

INSERT INTO tb_cliente_pagina (id, cliente_id, pagina_id) VALUES
  ('00000000-0000-0000-0000-000000000501', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_pagina WHERE slug = 'visao-geral-do-painel')),
  ('00000000-0000-0000-0000-000000000502', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_pagina WHERE slug = 'configuracoes-gerais')),
  ('00000000-0000-0000-0000-000000000503', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_pagina WHERE slug = 'gestao-de-usuarios')),
  ('00000000-0000-0000-0000-000000000504', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_pagina WHERE slug = 'cadastro-de-clientes')),
  ('00000000-0000-0000-0000-000000000505', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_pagina WHERE slug = 'vinculos-modulos-paginas')),
  ('00000000-0000-0000-0000-000000000506', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_pagina WHERE slug = 'cadastro-de-modulos')),
  ('00000000-0000-0000-0000-000000000507', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_pagina WHERE slug = 'consulta-de-documentos')),
  ('00000000-0000-0000-0000-000000000508', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_pagina WHERE slug = 'revisao-de-documento')),
  ('00000000-0000-0000-0000-000000000509', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_pagina WHERE slug = 'geracao-de-publicacao')),
  ('00000000-0000-0000-0000-000000000510', (SELECT id FROM tb_cliente WHERE slug = 'basa'),  (SELECT id FROM tb_pagina WHERE slug = 'historico-de-publicacoes')),
  ('00000000-0000-0000-0000-000000000511', (SELECT id FROM tb_cliente WHERE slug = 'brb'),   (SELECT id FROM tb_pagina WHERE slug = 'cadastro-de-agencia')),
  ('00000000-0000-0000-0000-000000000512', (SELECT id FROM tb_cliente WHERE slug = 'brb'),   (SELECT id FROM tb_pagina WHERE slug = 'consulta-de-documentos')),
  ('00000000-0000-0000-0000-000000000513', (SELECT id FROM tb_cliente WHERE slug = 'brb'),   (SELECT id FROM tb_pagina WHERE slug = 'geracao-de-publicacao')),
  ('00000000-0000-0000-0000-000000000514', (SELECT id FROM tb_cliente WHERE slug = 'brb'),   (SELECT id FROM tb_pagina WHERE slug = 'preview-do-pacote')),
  ('00000000-0000-0000-0000-000000000515', (SELECT id FROM tb_cliente WHERE slug = 'bmw'),   (SELECT id FROM tb_pagina WHERE slug = 'consulta-de-documentos')),
  ('00000000-0000-0000-0000-000000000516', (SELECT id FROM tb_cliente WHERE slug = 'bmw'),   (SELECT id FROM tb_pagina WHERE slug = 'revisao-de-documento')),
  ('00000000-0000-0000-0000-000000000517', (SELECT id FROM tb_cliente WHERE slug = 'bmw'),   (SELECT id FROM tb_pagina WHERE slug = 'aprovacao-e-devolucao')),
  ('00000000-0000-0000-0000-000000000518', (SELECT id FROM tb_cliente WHERE slug = 'caixa'), (SELECT id FROM tb_pagina WHERE slug = 'visao-geral-do-painel')),
  ('00000000-0000-0000-0000-000000000519', (SELECT id FROM tb_cliente WHERE slug = 'caixa'), (SELECT id FROM tb_pagina WHERE slug = 'cadastro-de-clientes')),
  ('00000000-0000-0000-0000-000000000520', (SELECT id FROM tb_cliente WHERE slug = 'caixa'), (SELECT id FROM tb_pagina WHERE slug = 'consulta-de-documentos')),
  ('00000000-0000-0000-0000-000000000521', (SELECT id FROM tb_cliente WHERE slug = 'caixa'), (SELECT id FROM tb_pagina WHERE slug = 'geracao-de-publicacao')),
  ('00000000-0000-0000-0000-000000000522', (SELECT id FROM tb_cliente WHERE slug = 'caixa'), (SELECT id FROM tb_pagina WHERE slug = 'historico-de-publicacoes'))
ON CONFLICT (cliente_id, pagina_id) DO NOTHING;

-- =============================================================================
-- PUBLICAÇÕES DE DEMONSTRAÇÃO
-- =============================================================================

INSERT INTO tb_publicacao (
  id, cliente_id, versao, status, quantidade_paginas, quantidade_modulos,
  arquivo_zip_nome, arquivo_zip_caminho, hash_pacote, observacao,
  created_at, updated_at, created_by, updated_by
) VALUES
  ('00000000-0000-0000-0000-000000000601', (SELECT id FROM tb_cliente WHERE slug = 'basa'),
   '1.0.0', 'SUCESSO', 8, 4, NULL, NULL, NULL,
   'Primeira publicação completa. Gere um novo pacote para testar o download real.',
   now() - interval '10 days', now() - interval '10 days', 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000602', (SELECT id FROM tb_cliente WHERE slug = 'basa'),
   '1.1.0', 'SUCESSO', 10, 5, NULL, NULL, NULL,
   'Atualização com novo módulo de relatórios e páginas de vínculos.',
   now() - interval '5 days', now() - interval '5 days', 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000603', (SELECT id FROM tb_cliente WHERE slug = 'basa'),
   '1.2.0', 'GERANDO', 0, 0, NULL, NULL, NULL,
   'Publicação em andamento — aguardando processamento.',
   now() - interval '30 minutes', now() - interval '30 minutes', 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000610', (SELECT id FROM tb_cliente WHERE slug = 'brb'),
   '1.0.0', 'SUCESSO', 4, 3, NULL, NULL, NULL,
   'Publicação inicial para BRB com módulos de Cadastros, Documentos e Publicações.',
   now() - interval '7 days', now() - interval '7 days', 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000611', (SELECT id FROM tb_cliente WHERE slug = 'brb'),
   '1.0.1', 'ERRO', 0, 0, NULL, NULL, NULL,
   'Falha ao gerar pacote: timeout no processo de compressão. Tente novamente.',
   now() - interval '2 days', now() - interval '2 days', 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000620', (SELECT id FROM tb_cliente WHERE slug = 'bmw'),
   '0.9.0', 'ERRO', 0, 0, NULL, NULL, NULL,
   'Falha simulada — sem páginas publicadas suficientes no momento da geração.',
   now() - interval '3 days', now() - interval '3 days', 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000630', (SELECT id FROM tb_cliente WHERE slug = 'caixa'),
   '2.0.0', 'SUCESSO', 5, 4, NULL, NULL, NULL,
   'Primeira publicação da Caixa Econômica Federal.',
   now() - interval '4 days', now() - interval '4 days', 'seed', 'seed')
ON CONFLICT (id) DO UPDATE SET
  status             = EXCLUDED.status,
  quantidade_paginas = EXCLUDED.quantidade_paginas,
  quantidade_modulos = EXCLUDED.quantidade_modulos,
  observacao         = EXCLUDED.observacao,
  updated_at         = now();

-- =============================================================================
-- USUÁRIOS
-- Hashes BCrypt (cost=10): admin→"admin" | editor→"editor" | revisor→"revisor"
-- =============================================================================

INSERT INTO tb_usuario (id, username, password, nome, roles, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  (
    '00000000-0000-0000-0000-000000000701',
    'admin',
    '$2b$10$hzUCFIZ3npCft.La1cPYB.W2qGRIBGYOsN5ndGlsXbJtJ1L7Ma4Ei',
    'Administrador', 'ADMIN,EDITOR',
    true, now(), now(), 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000702',
    'editor',
    '$2b$10$p22zwg4H9Iu.0V3CVX//y.IVPSjrZWgZfdW0giT2J8oHAKYeNxA1a',
    'Editor Padrão', 'EDITOR',
    true, now(), now(), 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000703',
    'revisor',
    '$2b$10$p22zwg4H9Iu.0V3CVX//y.IVPSjrZWgZfdW0giT2J8oHAKYeNxA1a',
    'Revisor de Conteúdo', 'EDITOR',
    true, now(), now(), 'seed', 'seed'
  )
ON CONFLICT (username) DO NOTHING;

-- =============================================================================
-- CHANGELOG DE PUBLICAÇÕES
-- =============================================================================

INSERT INTO tb_publicacao_changelog (id, publicacao_id, pagina_id, pagina_titulo, tipo_mudanca, created_at) VALUES
  ('00000000-0000-0000-0000-000000000871', '00000000-0000-0000-0000-000000000601', '00000000-0000-0000-0000-000000000301', 'Visão geral do painel',              'ADICIONADO', now() - interval '10 days'),
  ('00000000-0000-0000-0000-000000000872', '00000000-0000-0000-0000-000000000601', '00000000-0000-0000-0000-000000000310', 'Cadastro de clientes',               'ADICIONADO', now() - interval '10 days'),
  ('00000000-0000-0000-0000-000000000873', '00000000-0000-0000-0000-000000000601', '00000000-0000-0000-0000-000000000320', 'Consulta de documentos',             'ADICIONADO', now() - interval '10 days'),
  ('00000000-0000-0000-0000-000000000874', '00000000-0000-0000-0000-000000000602', '00000000-0000-0000-0000-000000000303', 'Configurações gerais',               'ADICIONADO', now() - interval '5 days'),
  ('00000000-0000-0000-0000-000000000875', '00000000-0000-0000-0000-000000000602', '00000000-0000-0000-0000-000000000340', 'Relatório de cobertura por cliente', 'ADICIONADO', now() - interval '5 days'),
  ('00000000-0000-0000-0000-000000000876', '00000000-0000-0000-0000-000000000610', '00000000-0000-0000-0000-000000000312', 'Cadastro de agência',                'ADICIONADO', now() - interval '7 days'),
  ('00000000-0000-0000-0000-000000000877', '00000000-0000-0000-0000-000000000610', '00000000-0000-0000-0000-000000000320', 'Consulta de documentos',             'ADICIONADO', now() - interval '7 days'),
  ('00000000-0000-0000-0000-000000000878', '00000000-0000-0000-0000-000000000630', '00000000-0000-0000-0000-000000000301', 'Visão geral do painel',              'ADICIONADO', now() - interval '4 days'),
  ('00000000-0000-0000-0000-000000000879', '00000000-0000-0000-0000-000000000630', '00000000-0000-0000-0000-000000000320', 'Consulta de documentos',             'ADICIONADO', now() - interval '4 days')
ON CONFLICT (id) DO NOTHING;
