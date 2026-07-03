-- =============================================================================
-- MÓDULO: rbac | SEED: expansão do catálogo para cobrir todos os controllers
--
-- Completa V8 adicionando funcionalidades que ainda não tinham permissão
-- granular, agora que os controllers passam a usar @PreAuthorize("hasAuthority")
-- em vez de role-based.
--
-- Novas funcionalidades:
--   DOC_FLOW              EMPRESA          (configuração da empresa proprietária)
--   RELEASE_ORCHESTRATOR  CLIENTE_RO       (cliente do orquestrador — distinto do CLIENTE do DocFlow)
--                         ENTREGA          (entregas e sub-recursos)
--                         PROXIMA_ENTREGA  (agenda de próximas entregas)
--
-- Sub-recursos (contatos, config-entrega, releases/itens, entregas/modulos…)
-- compartilham a permissão do agregado pai. Ações especiais (ex.: GERAR_PDF,
-- CANCELAR) podem ser adicionadas em V10+ quando o controller precisar.
--
-- Distribuição idêntica ao V8:
--   ADMIN   → todas as permissões ativas
--   EDITOR  → CRUD completo nas funcionalidades novas
--   REVISOR → apenas :LER
--   LEITOR  → apenas :LER
-- =============================================================================

-- -----------------------------------------------------------------------------
-- tb_funcionalidade — 1 nova em DOC_FLOW + 3 em RELEASE_ORCHESTRATOR
-- -----------------------------------------------------------------------------

INSERT INTO tb_funcionalidade (id, dominio_id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  -- DOC_FLOW
  ('00000000-0000-0000-0200-000000000021', '00000000-0000-0000-0100-000000000003', 'EMPRESA',         'Empresa',          'Configurações da empresa proprietária do portal.',     true, now(), now(), 'seed', 'seed'),
  -- RELEASE_ORCHESTRATOR
  ('00000000-0000-0000-0200-000000000022', '00000000-0000-0000-0100-000000000004', 'CLIENTE_RO',      'Cliente RO',       'Clientes do orquestrador (contatos, config, produtos).', true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000023', '00000000-0000-0000-0100-000000000004', 'ENTREGA',         'Entrega',          'Entregas executadas e seus sub-recursos.',             true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000024', '00000000-0000-0000-0100-000000000004', 'PROXIMA_ENTREGA', 'Próxima entrega',  'Agenda de próximas entregas planejadas.',              true, now(), now(), 'seed', 'seed')
ON CONFLICT (dominio_id, codigo) DO UPDATE SET
  nome = EXCLUDED.nome, descricao = EXCLUDED.descricao, ativo = EXCLUDED.ativo, updated_at = now();

-- -----------------------------------------------------------------------------
-- tb_permissao — CRUD por funcionalidade ativa (idempotente)
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
-- -----------------------------------------------------------------------------

-- ADMIN → tudo
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000801', p.id
FROM tb_permissao p
WHERE p.ativo = true
ON CONFLICT DO NOTHING;

-- LEITOR → só :LER
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000803', p.id
FROM tb_permissao p
WHERE p.acao = 'LER'
  AND p.ativo = true
ON CONFLICT DO NOTHING;

-- REVISOR → só :LER
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000804', p.id
FROM tb_permissao p
WHERE p.ativo = true
  AND p.acao = 'LER'
ON CONFLICT DO NOTHING;

-- EDITOR → CRUD completo nas novas funcionalidades (mesmo critério do V8).
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000802', p.id
FROM tb_permissao p
JOIN tb_funcionalidade f ON f.id = p.funcionalidade_id
WHERE p.ativo = true
  AND f.codigo IN ('EMPRESA', 'CLIENTE_RO', 'ENTREGA', 'PROXIMA_ENTREGA')
  AND p.acao   IN ('LER', 'CRIAR', 'EDITAR', 'EXCLUIR')
ON CONFLICT DO NOTHING;
