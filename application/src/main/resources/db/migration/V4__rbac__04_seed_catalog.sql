-- =============================================================================
-- MÓDULO: rbac | SEED: catálogo + grupos
-- Espelha softon-portal-web/.../seguranca/services/mock/seed.ts
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Domínios
-- -----------------------------------------------------------------------------

INSERT INTO tb_dominio (id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0100-000000000001', 'SEGURANCA',            'Segurança',            'Identidade, grupos, permissões e escopos.',              true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0100-000000000002', 'SISTEMA',              'Sistema',              'Configurações gerais.',                                  true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0100-000000000003', 'DOC_FLOW',             'Doc Flow',             'Manuais de cliente.',                                    true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0100-000000000004', 'RELEASE_ORCHESTRATOR', 'Release Orchestrator', 'Releases, entregas a clientes e orquestração de pacotes.', true, now(), now(), 'seed', 'seed')
ON CONFLICT (codigo) DO UPDATE SET
  nome = EXCLUDED.nome, descricao = EXCLUDED.descricao, ativo = EXCLUDED.ativo, updated_at = now();

-- -----------------------------------------------------------------------------
-- Funcionalidades
-- -----------------------------------------------------------------------------

INSERT INTO tb_funcionalidade (id, dominio_id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  -- SEGURANCA
  ('00000000-0000-0000-0200-000000000001', '00000000-0000-0000-0100-000000000001', 'USUARIO',            'Usuário',             NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000002', '00000000-0000-0000-0100-000000000001', 'GRUPO_ACESSO',       'Grupo de acesso',     NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000003', '00000000-0000-0000-0100-000000000001', 'DOMINIO',            'Domínio',             NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000004', '00000000-0000-0000-0100-000000000001', 'FUNCIONALIDADE',     'Funcionalidade',      NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000005', '00000000-0000-0000-0100-000000000001', 'PERMISSAO',          'Permissão',           NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000006', '00000000-0000-0000-0100-000000000001', 'ESCOPO',             'Escopo de acesso',    NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000007', '00000000-0000-0000-0100-000000000001', 'AUDITORIA',          'Auditoria',           NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000008', '00000000-0000-0000-0100-000000000001', 'HISTORICO_LOGIN',    'Histórico de login',  NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000009', '00000000-0000-0000-0100-000000000001', 'POLITICA_SENHA',     'Política de senha',   NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000010', '00000000-0000-0000-0100-000000000001', 'SESSAO',             'Sessão de usuário',   NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000011', '00000000-0000-0000-0100-000000000001', 'ACESSO_TEMPORARIO',  'Acesso temporário',   NULL, true, now(), now(), 'seed', 'seed'),
  -- SISTEMA
  ('00000000-0000-0000-0200-000000000012', '00000000-0000-0000-0100-000000000002', 'CONFIGURACAO',       'Configuração',        NULL, true, now(), now(), 'seed', 'seed'),
  -- DOC_FLOW
  ('00000000-0000-0000-0200-000000000013', '00000000-0000-0000-0100-000000000003', 'CLIENTE',            'Cliente',             NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000014', '00000000-0000-0000-0100-000000000003', 'PROJETO',            'Projeto',             NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000015', '00000000-0000-0000-0100-000000000003', 'MODULO',             'Módulo',              NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000016', '00000000-0000-0000-0100-000000000003', 'PAGINA',             'Página',              NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000017', '00000000-0000-0000-0100-000000000003', 'PUBLICACAO',         'Publicação',          NULL, true, now(), now(), 'seed', 'seed'),
  -- RELEASE_ORCHESTRATOR
  ('00000000-0000-0000-0200-000000000018', '00000000-0000-0000-0100-000000000004', 'RELEASE',            'Release',             NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000019', '00000000-0000-0000-0100-000000000004', 'TEMPLATE',           'Template',            NULL, true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000020', '00000000-0000-0000-0100-000000000004', 'PRODUTO',            'Produto',             NULL, true, now(), now(), 'seed', 'seed')
ON CONFLICT (dominio_id, codigo) DO UPDATE SET
  nome = EXCLUDED.nome, ativo = EXCLUDED.ativo, updated_at = now();

-- -----------------------------------------------------------------------------
-- Permissões CRUD (FUNC:ACAO)
-- -----------------------------------------------------------------------------

INSERT INTO tb_permissao (id, funcionalidade_id, acao, codigo, descricao, ativo, created_at, updated_at, created_by, updated_by)
SELECT
  gen_random_uuid(),
  f.id,
  a.acao,
  f.codigo || ':' || a.acao,
  NULL,
  true,
  now(),
  now(),
  'seed',
  'seed'
FROM tb_funcionalidade f
CROSS JOIN (VALUES ('LER'), ('CRIAR'), ('EDITAR'), ('EXCLUIR')) AS a(acao)
WHERE f.ativo = true
ON CONFLICT (codigo) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Permissões especiais (fora do CRUD padrão)
-- -----------------------------------------------------------------------------

INSERT INTO tb_permissao (id, funcionalidade_id, acao, codigo, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000002', 'VINCULAR_PERMISSAO', 'GRUPO_ACESSO:VINCULAR_PERMISSAO', NULL, true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000008', 'VISUALIZAR',         'HISTORICO_LOGIN:VISUALIZAR',       NULL, true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000007', 'VISUALIZAR',         'AUDITORIA:VISUALIZAR',             NULL, true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000010', 'REVOGAR',            'SESSAO:REVOGAR',                   NULL, true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000011', 'REVOGAR',            'ACESSO_TEMPORARIO:REVOGAR',        NULL, true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000001', 'RESETAR_SENHA',      'USUARIO:RESETAR_SENHA',            NULL, true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000001', 'BLOQUEAR',           'USUARIO:BLOQUEAR',                 NULL, true, now(), now(), 'seed', 'seed')
ON CONFLICT (codigo) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Grupos de acesso
-- -----------------------------------------------------------------------------

INSERT INTO tb_grupo (id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0000-000000000801', 'ADMIN',   'Administradores', 'Grupo com todas as permissões.',                  true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000802', 'EDITOR',  'Editores',        'CRUD completo no Doc Flow.',                       true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000803', 'LEITOR',  'Leitores',        'Somente permissões de leitura (LER).',             true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000804', 'REVISOR', 'Revisores',       'Leitura no Doc Flow + edição de páginas.',         true, now(), now(), 'seed', 'seed')
ON CONFLICT (codigo) DO UPDATE SET
  nome = EXCLUDED.nome, descricao = EXCLUDED.descricao, ativo = EXCLUDED.ativo, updated_at = now();

-- ADMIN → todas as permissões ativas
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000801', p.id
FROM tb_permissao p
WHERE p.ativo = true
ON CONFLICT DO NOTHING;

-- EDITOR → CRUD Doc Flow
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000802', p.id
FROM tb_permissao p
JOIN tb_funcionalidade f ON f.id = p.funcionalidade_id
WHERE f.codigo IN ('CLIENTE', 'PROJETO', 'MODULO', 'PAGINA', 'PUBLICACAO')
  AND p.ativo = true
ON CONFLICT DO NOTHING;

-- LEITOR → somente LER
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000803', p.id
FROM tb_permissao p
WHERE p.acao = 'LER'
  AND p.ativo = true
ON CONFLICT DO NOTHING;

-- REVISOR → LER em todo Doc Flow + EDITAR em PAGINA
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000804', p.id
FROM tb_permissao p
JOIN tb_funcionalidade f ON f.id = p.funcionalidade_id
WHERE f.codigo IN ('CLIENTE', 'PROJETO', 'MODULO', 'PAGINA', 'PUBLICACAO')
  AND p.acao = 'LER'
  AND p.ativo = true
ON CONFLICT DO NOTHING;

INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000804', p.id
FROM tb_permissao p
WHERE p.codigo = 'PAGINA:EDITAR'
  AND p.ativo = true
ON CONFLICT DO NOTHING;
