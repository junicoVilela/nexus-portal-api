-- Cliente demonstrativo vinculado ao projeto/módulo de exemplo (V36), com páginas publicadas.

INSERT INTO tb_cliente (id, nome, slug, ativo, created_by, updated_by)
SELECT 'a0000000-0000-4000-8000-000000000010', 'Cliente Exemplo', 'cliente-exemplo', TRUE, 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM tb_cliente WHERE slug = 'cliente-exemplo');

INSERT INTO tb_cliente_projeto (id, cliente_id, projeto_id)
SELECT 'a0000000-0000-4000-8000-000000000011',
       'a0000000-0000-4000-8000-000000000010',
       'a0000000-0000-4000-8000-000000000001'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_cliente_projeto
  WHERE cliente_id = 'a0000000-0000-4000-8000-000000000010'
    AND projeto_id = 'a0000000-0000-4000-8000-000000000001'
);

INSERT INTO tb_cliente_modulo (id, cliente_id, modulo_id)
SELECT 'a0000000-0000-4000-8000-000000000012',
       'a0000000-0000-4000-8000-000000000010',
       'a0000000-0000-4000-8000-000000000002'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_cliente_modulo
  WHERE cliente_id = 'a0000000-0000-4000-8000-000000000010'
    AND modulo_id = 'a0000000-0000-4000-8000-000000000002'
);

UPDATE tb_pagina
SET status = 'PUBLICADO', published_at = now()
WHERE slug IN ('exemplo-operacoes', 'exemplo-lista', 'exemplo-incluir', 'exemplo-editar')
  AND status <> 'PUBLICADO';
