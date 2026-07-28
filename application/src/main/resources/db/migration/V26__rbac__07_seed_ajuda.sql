INSERT INTO tb_funcionalidade
  (id, dominio_id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0200-000000000025',
   '00000000-0000-0000-0100-000000000003',
   'AJUDA', 'Ajuda', 'Conteúdo, tours e métricas da Central de Ajuda.',
   true, now(), now(), 'seed', 'seed')
ON CONFLICT (dominio_id, codigo) DO UPDATE SET
  nome = EXCLUDED.nome, descricao = EXCLUDED.descricao, ativo = EXCLUDED.ativo, updated_at = now();

INSERT INTO tb_permissao
  (id, funcionalidade_id, acao, codigo, descricao, ativo, created_at, updated_at, created_by, updated_by)
SELECT gen_random_uuid(), f.id, a.acao, f.codigo || ':' || a.acao, NULL, true, now(), now(), 'seed', 'seed'
FROM tb_funcionalidade f
CROSS JOIN (VALUES ('LER'), ('CRIAR'), ('EDITAR'), ('EXCLUIR')) AS a(acao)
WHERE f.codigo = 'AJUDA'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT g.id, p.id
FROM tb_grupo g
CROSS JOIN tb_permissao p
WHERE p.codigo = 'AJUDA:LER'
  AND g.codigo IN ('ADMIN', 'EDITOR', 'REVISOR', 'LEITOR')
ON CONFLICT DO NOTHING;

INSERT INTO tb_grupo_permissao (grupo_id, permissao_id)
SELECT g.id, p.id
FROM tb_grupo g
CROSS JOIN tb_permissao p
WHERE p.codigo IN ('AJUDA:CRIAR', 'AJUDA:EDITAR', 'AJUDA:EXCLUIR')
  AND g.codigo IN ('ADMIN', 'EDITOR')
ON CONFLICT DO NOTHING;
