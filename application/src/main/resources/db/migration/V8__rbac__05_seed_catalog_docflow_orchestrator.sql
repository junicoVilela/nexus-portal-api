-- =============================================================================
-- MÓDULO: rbac | SEED: catálogo + grupos para DOC_FLOW e RELEASE_ORCHESTRATOR
--
-- Complementa o seed inicial (V5) adicionando os domínios funcionais usados
-- pelos shells do frontend (`docflow-shell` e `release-orchestrator-shell`).
--
-- Funcionalidades cobertas:
--   DOC_FLOW              CLIENTE, PROJETO, MODULO, PAGINA, PUBLICACAO
--   RELEASE_ORCHESTRATOR  RELEASE, PRODUTO, TEMPLATE
--
-- Distribuição de permissões pelos grupos:
--   ADMIN   → todas as permissões ativas (catch-all, idempotente via ON CONFLICT)
--   EDITOR  → CRUD completo em todas as funcionalidades novas
--   REVISOR → apenas :LER em todas as funcionalidades novas
--   LEITOR  → apenas :LER em todas as funcionalidades novas
-- =============================================================================

-- -----------------------------------------------------------------------------
-- tb_dominio — DOC_FLOW + RELEASE_ORCHESTRATOR
-- -----------------------------------------------------------------------------

INSERT INTO tb_dominio (id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0100-000000000003', 'DOC_FLOW',             'Doc Flow',             'Manuais do usuário: clientes, projetos, módulos, páginas e publicações.', true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0100-000000000004', 'RELEASE_ORCHESTRATOR', 'Release Orchestrator', 'Releases, produtos e templates de entrega.',                              true, now(), now(), 'seed', 'seed')
ON CONFLICT (codigo) DO UPDATE SET
  nome = EXCLUDED.nome, descricao = EXCLUDED.descricao, ativo = EXCLUDED.ativo, updated_at = now();

-- -----------------------------------------------------------------------------
-- tb_funcionalidade — 5 funcs de DOC_FLOW + 3 de RELEASE_ORCHESTRATOR
-- -----------------------------------------------------------------------------

INSERT INTO tb_funcionalidade (id, dominio_id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  -- DOC_FLOW
  ('00000000-0000-0000-0200-000000000013', '00000000-0000-0000-0100-000000000003', 'CLIENTE',    'Cliente',    'Clientes consumidores dos manuais.',           true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000014', '00000000-0000-0000-0100-000000000003', 'PROJETO',    'Projeto',    'Projetos vinculados a clientes.',              true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000015', '00000000-0000-0000-0100-000000000003', 'MODULO',     'Módulo',     'Módulos que agrupam páginas de manual.',       true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000016', '00000000-0000-0000-0100-000000000003', 'PAGINA',     'Página',     'Páginas individuais do manual.',               true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000017', '00000000-0000-0000-0100-000000000003', 'PUBLICACAO', 'Publicação', 'Snapshots publicados de um manual.',           true, now(), now(), 'seed', 'seed'),
  -- RELEASE_ORCHESTRATOR
  ('00000000-0000-0000-0200-000000000018', '00000000-0000-0000-0100-000000000004', 'RELEASE',    'Release',    'Releases de versão entregues a clientes.',     true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000019', '00000000-0000-0000-0100-000000000004', 'PRODUTO',    'Produto',    'Produtos do catálogo do orquestrador.',        true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000020', '00000000-0000-0000-0100-000000000004', 'TEMPLATE',   'Template',   'Templates de release reutilizáveis.',          true, now(), now(), 'seed', 'seed')
ON CONFLICT (dominio_id, codigo) DO UPDATE SET
  nome = EXCLUDED.nome, descricao = EXCLUDED.descricao, ativo = EXCLUDED.ativo, updated_at = now();

-- -----------------------------------------------------------------------------
-- tb_permissao — CRUD (FUNC:LER/CRIAR/EDITAR/EXCLUIR) por funcionalidade ativa
-- Re-executa o cross-join do V5; ON CONFLICT mantém idempotência para as funcs
-- já existentes e materializa as 4 ações para cada funcionalidade nova.
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
-- tb_grupo_permissao — propaga novas permissões para os 4 grupos base
-- Mantém a mesma semântica do V5 e, graças ao ON CONFLICT DO NOTHING, não
-- altera vínculos já existentes — apenas adiciona o que falta.
-- -----------------------------------------------------------------------------

-- ADMIN → todas as permissões ativas (catch-all idempotente).
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000801', p.id
FROM tb_permissao p
WHERE p.ativo = true
ON CONFLICT DO NOTHING;

-- LEITOR → apenas ações :LER em todas as funcionalidades ativas.
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000803', p.id
FROM tb_permissao p
WHERE p.acao = 'LER'
  AND p.ativo = true
ON CONFLICT DO NOTHING;

-- REVISOR → :LER em todas as funcionalidades ativas (mantém o papel de
-- auditor já consolidado em V5 para SEGURANCA + SISTEMA).
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000804', p.id
FROM tb_permissao p
WHERE p.ativo = true
  AND p.acao = 'LER'
ON CONFLICT DO NOTHING;

-- EDITOR → CRUD completo nas funcionalidades dos novos domínios.
-- Não toca em outros domínios já cobertos pelo V5.
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000802', p.id
FROM tb_permissao p
JOIN tb_funcionalidade f ON f.id = p.funcionalidade_id
JOIN tb_dominio d        ON d.id = f.dominio_id
WHERE p.ativo = true
  AND d.codigo IN ('DOC_FLOW', 'RELEASE_ORCHESTRATOR')
  AND p.acao   IN ('LER', 'CRIAR', 'EDITAR', 'EXCLUIR')
ON CONFLICT DO NOTHING;
