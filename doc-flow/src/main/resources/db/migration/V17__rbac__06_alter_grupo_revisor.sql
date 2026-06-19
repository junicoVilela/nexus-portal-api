-- =============================================================================
-- MÓDULO: rbac | ETAPA 6: alterações incrementais
-- Adiciona grupo REVISOR e corrige seed do usuário `revisor`:
--   - V12 gravou o hash BCrypt de "editor" para o usuário revisor (bug);
--     este script restaura o hash de "revisor".
--   - Movemos `revisor` do grupo EDITOR para o novo grupo REVISOR.
-- =============================================================================

-- 1) Grupo REVISOR
INSERT INTO tb_grupo (id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0000-000000000804', 'REVISOR', 'Revisores', 'Leitura no Doc Flow + edição de páginas.', true, now(), now(), 'seed', 'seed')
ON CONFLICT (codigo) DO UPDATE SET
  nome = EXCLUDED.nome, descricao = EXCLUDED.descricao, ativo = EXCLUDED.ativo, updated_at = now();

-- 2) Permissões do REVISOR: LER em todo o Doc Flow
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000804', p.id
FROM tb_permissao p
JOIN tb_funcionalidade f ON f.id = p.funcionalidade_id
WHERE f.codigo IN ('CLIENTE', 'PROJETO', 'MODULO', 'PAGINA', 'PUBLICACAO')
  AND p.acao = 'LER'
  AND p.ativo = true
ON CONFLICT DO NOTHING;

-- 3) Permissão extra: editar páginas (fluxo de revisão)
INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT '00000000-0000-0000-0000-000000000804', p.id
FROM tb_permissao p
WHERE p.codigo = 'PAGINA:EDITAR'
  AND p.ativo = true
ON CONFLICT DO NOTHING;

-- 4) Corrige usuário `revisor`: hash de "revisor" + role REVISOR
UPDATE tb_usuario
SET password = '$2b$10$XQVyjh5JWjR2mK.xiOJo7uw8Jv.TpkzG68/pIM9TEnUxCxYuWq6a6',
    roles    = 'REVISOR',
    updated_at = now()
WHERE username = 'revisor';

-- 5) Move vínculo: EDITOR (802) → REVISOR (804)
DELETE FROM tb_grupo_usuario
WHERE grupo_id   = '00000000-0000-0000-0000-000000000802'
  AND usuario_id = '00000000-0000-0000-0000-000000000703';

INSERT INTO tb_grupo_usuario (grupo_id, usuario_id)
VALUES ('00000000-0000-0000-0000-000000000804', '00000000-0000-0000-0000-000000000703')
ON CONFLICT DO NOTHING;
