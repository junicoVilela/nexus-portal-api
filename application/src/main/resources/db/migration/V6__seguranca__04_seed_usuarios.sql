-- =============================================================================
-- MÓDULO: seguranca | SEED: usuários iniciais
-- Credenciais: admin/admin | editor/editor | revisor/revisor | leitor/leitor
-- Hashes BCrypt (cost=10): admin→"admin" | editor→"editor" | revisor→"revisor"
--                          leitor→"leitor"
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
    '$2b$10$XQVyjh5JWjR2mK.xiOJo7uw8Jv.TpkzG68/pIM9TEnUxCxYuWq6a6',
    'Revisor de Conteúdo', 'REVISOR',
    true, now(), now(), 'seed', 'seed'
  ),
  (
    '00000000-0000-0000-0000-000000000704',
    'leitor',
    '$2b$10$tMgFDU5L4cWEEKScZjomT.l8sPckl6SgAOP9p2OkmSnLSggSWAWzi',
    'Leitor Padrão', 'LEITOR',
    true, now(), now(), 'seed', 'seed'
  )
ON CONFLICT (username) DO NOTHING;
