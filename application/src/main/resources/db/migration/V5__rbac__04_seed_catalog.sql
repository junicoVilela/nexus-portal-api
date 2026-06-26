-- =============================================================================
-- MÓDULO: rbac | SEED: catálogo + grupos do módulo de segurança/sistema
--
-- Cobre todas as 6 tabelas do RBAC restritas ao escopo de SEGURANCA + SISTEMA:
--   * tb_dominio
--   * tb_funcionalidade
--   * tb_permissao             (CRUD + ações especiais)
--   * tb_grupo                  (ADMIN, EDITOR, LEITOR, REVISOR)
--   * tb_grupo_permissao        (distribuição por papel)
--   * tb_grupo_usuario          → vínculos em V7 (depende de V6 também)
--
-- Distribuição de permissões pelos grupos:
--   ADMIN   → todas as permissões ativas (full)
--   EDITOR  → gestão operacional: CRUD em USUARIO/GRUPO_ACESSO/ACESSO_TEMPORARIO
--             + ações especiais (RESETAR_SENHA, BLOQUEAR, VINCULAR_PERMISSAO,
--             SESSAO:REVOGAR, ACESSO_TEMPORARIO:REVOGAR) + LER no resto do
--             catálogo + LER/EDITAR em POLITICA_SENHA + SESSAO leitura
--   REVISOR → papel de auditor: LER em todas as funcionalidades +
--             HISTORICO_LOGIN:VISUALIZAR + AUDITORIA:VISUALIZAR
--   LEITOR  → apenas permissões :LER
--
-- Funcionalidades de outros módulos (DOC_FLOW, RELEASE_ORCHESTRATOR, etc.)
-- entram sob demanda em V8+ junto com a expansão dos grupos.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- tb_dominio — SEGURANCA + SISTEMA
-- -----------------------------------------------------------------------------

INSERT INTO tb_dominio (id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0100-000000000001', 'SEGURANCA', 'Segurança', 'Identidade, grupos, permissões e escopos.', true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0100-000000000002', 'SISTEMA',   'Sistema',   'Configurações gerais.',                    true, now(), now(), 'seed', 'seed')
ON CONFLICT (codigo) DO UPDATE SET
  nome = EXCLUDED.nome, descricao = EXCLUDED.descricao, ativo = EXCLUDED.ativo, updated_at = now();

-- -----------------------------------------------------------------------------
-- tb_funcionalidade — 11 funcs de SEGURANCA + 1 de SISTEMA
-- -----------------------------------------------------------------------------

INSERT INTO tb_funcionalidade (id, dominio_id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  -- SEGURANCA
  ('00000000-0000-0000-0200-000000000001', '00000000-0000-0000-0100-000000000001', 'USUARIO',            'Usuário',            'Identidade individual.',                                   true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000002', '00000000-0000-0000-0100-000000000001', 'GRUPO_ACESSO',       'Grupo de acesso',    'Coleção de permissões atribuídas a usuários.',             true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000003', '00000000-0000-0000-0100-000000000001', 'DOMINIO',            'Domínio',            'Agrupamento lógico do catálogo de permissões.',            true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000004', '00000000-0000-0000-0100-000000000001', 'FUNCIONALIDADE',     'Funcionalidade',     'Recurso protegido dentro de um domínio.',                  true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000005', '00000000-0000-0000-0100-000000000001', 'PERMISSAO',          'Permissão',          'Ação (FUNC:ACAO) atribuível a um grupo.',                  true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000006', '00000000-0000-0000-0100-000000000001', 'ESCOPO',             'Escopo de acesso',   'Restringe o alcance dos dados acessíveis.',                true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000007', '00000000-0000-0000-0100-000000000001', 'AUDITORIA',          'Auditoria',          'Trilha de ações realizadas no sistema.',                   true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000008', '00000000-0000-0000-0100-000000000001', 'HISTORICO_LOGIN',    'Histórico de login', 'Tentativas de autenticação bem ou mal sucedidas.',         true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000009', '00000000-0000-0000-0100-000000000001', 'POLITICA_SENHA',     'Política de senha',  'Regras de complexidade, expiração e bloqueio.',            true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000010', '00000000-0000-0000-0100-000000000001', 'SESSAO',             'Sessão de usuário',  'Sessões ativas/expiradas com possibilidade de revogação.', true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0200-000000000011', '00000000-0000-0000-0100-000000000001', 'ACESSO_TEMPORARIO',  'Acesso temporário',  'Concessão limitada por tempo a um recurso.',               true, now(), now(), 'seed', 'seed'),
  -- SISTEMA
  ('00000000-0000-0000-0200-000000000012', '00000000-0000-0000-0100-000000000002', 'CONFIGURACAO',       'Configuração',       'Parâmetros globais do sistema.',                           true, now(), now(), 'seed', 'seed')
ON CONFLICT (dominio_id, codigo) DO UPDATE SET
  nome = EXCLUDED.nome, descricao = EXCLUDED.descricao, ativo = EXCLUDED.ativo, updated_at = now();

-- -----------------------------------------------------------------------------
-- tb_permissao — CRUD (FUNC:LER/CRIAR/EDITAR/EXCLUIR) por funcionalidade ativa
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
-- tb_permissao — ações especiais (fora do CRUD padrão)
-- -----------------------------------------------------------------------------

INSERT INTO tb_permissao (id, funcionalidade_id, acao, codigo, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000002', 'VINCULAR_PERMISSAO', 'GRUPO_ACESSO:VINCULAR_PERMISSAO', 'Atribuir/remover permissões de um grupo.',     true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000008', 'VISUALIZAR',         'HISTORICO_LOGIN:VISUALIZAR',       'Consultar histórico de tentativas de login.',  true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000007', 'VISUALIZAR',         'AUDITORIA:VISUALIZAR',             'Consultar trilha de auditoria de ações.',      true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000010', 'REVOGAR',            'SESSAO:REVOGAR',                   'Encerrar sessão ativa de outro usuário.',      true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000011', 'REVOGAR',            'ACESSO_TEMPORARIO:REVOGAR',        'Cancelar acesso temporário antes do prazo.',   true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000001', 'RESETAR_SENHA',      'USUARIO:RESETAR_SENHA',            'Forçar nova senha para um usuário.',           true, now(), now(), 'seed', 'seed'),
  (gen_random_uuid(), '00000000-0000-0000-0200-000000000001', 'BLOQUEAR',           'USUARIO:BLOQUEAR',                 'Bloquear/desbloquear usuário.',                true, now(), now(), 'seed', 'seed')
ON CONFLICT (codigo) DO NOTHING;

-- -----------------------------------------------------------------------------
-- tb_grupo — 4 grupos base
-- -----------------------------------------------------------------------------

INSERT INTO tb_grupo (id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0000-000000000801', 'ADMIN',   'Administradores', 'Acesso completo: todas as permissões ativas do catálogo.',                         true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000802', 'EDITOR',  'Editores',        'Gestão operacional de usuários, grupos, sessões e política de senha.',             true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000803', 'LEITOR',  'Leitores',        'Apenas permissões de leitura (:LER).',                                              true, now(), now(), 'seed', 'seed'),
  ('00000000-0000-0000-0000-000000000804', 'REVISOR', 'Revisores',       'Papel de auditoria: leitura do catálogo + visualização de histórico e auditoria.', true, now(), now(), 'seed', 'seed')
ON CONFLICT (codigo) DO UPDATE SET
  nome = EXCLUDED.nome, descricao = EXCLUDED.descricao, ativo = EXCLUDED.ativo, updated_at = now();

-- -----------------------------------------------------------------------------
-- tb_grupo_permissao — distribuição por grupo
-- -----------------------------------------------------------------------------

-- ADMIN → todas as permissões ativas (55 hoje no escopo SEGURANCA+SISTEMA).
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000801', p.id
FROM tb_permissao p
WHERE p.ativo = true
ON CONFLICT DO NOTHING;

-- LEITOR → apenas ações :LER (uma por funcionalidade ativa).
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000803', p.id
FROM tb_permissao p
WHERE p.acao = 'LER'
  AND p.ativo = true
ON CONFLICT DO NOTHING;

-- REVISOR → :LER em todas as funcionalidades + ações de visualização de
-- histórico e auditoria. Não cria/edita/exclui nada.
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000804', p.id
FROM tb_permissao p
WHERE p.ativo = true
  AND (p.acao = 'LER'
       OR p.codigo IN ('HISTORICO_LOGIN:VISUALIZAR', 'AUDITORIA:VISUALIZAR'))
ON CONFLICT DO NOTHING;

-- EDITOR → gestão operacional do módulo de segurança.
--
-- Cobertura por funcionalidade:
--   USUARIO            CRUD + RESETAR_SENHA + BLOQUEAR
--   GRUPO_ACESSO       CRUD + VINCULAR_PERMISSAO
--   ACESSO_TEMPORARIO  CRUD + REVOGAR
--   SESSAO             LER + REVOGAR
--   POLITICA_SENHA     LER + EDITAR
--   AUDITORIA          LER
--   HISTORICO_LOGIN    LER
--   DOMINIO            LER  (manuseio do catálogo é responsabilidade do ADMIN)
--   FUNCIONALIDADE     LER
--   PERMISSAO          LER
--   ESCOPO             LER
--   CONFIGURACAO       LER
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000802', p.id
FROM tb_permissao p
JOIN tb_funcionalidade f ON f.id = p.funcionalidade_id
WHERE p.ativo = true
  AND (
        -- CRUD completo nas funcionalidades que o editor opera
        (f.codigo IN ('USUARIO', 'GRUPO_ACESSO', 'ACESSO_TEMPORARIO')
           AND p.acao IN ('LER', 'CRIAR', 'EDITAR', 'EXCLUIR'))
        -- Acessos pontuais
     OR (f.codigo = 'SESSAO'          AND p.acao = 'LER')
     OR (f.codigo = 'POLITICA_SENHA'  AND p.acao IN ('LER', 'EDITAR'))
     OR (f.codigo = 'AUDITORIA'       AND p.acao = 'LER')
     OR (f.codigo = 'HISTORICO_LOGIN' AND p.acao = 'LER')
        -- LER no restante do catálogo
     OR (f.codigo IN ('DOMINIO', 'FUNCIONALIDADE', 'PERMISSAO', 'ESCOPO', 'CONFIGURACAO')
           AND p.acao = 'LER')
        -- Ações especiais
     OR p.codigo IN (
          'USUARIO:RESETAR_SENHA',
          'USUARIO:BLOQUEAR',
          'GRUPO_ACESSO:VINCULAR_PERMISSAO',
          'SESSAO:REVOGAR',
          'ACESSO_TEMPORARIO:REVOGAR'
        )
      )
ON CONFLICT DO NOTHING;
