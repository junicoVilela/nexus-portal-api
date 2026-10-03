-- Permissões próprias do assistente de IA (AI-704).
--
-- Até aqui o assistente ficava sob PAGINA:CRIAR: quem cria página manualmente
-- também gastava tokens, importava documentos e aplicava propostas. Com ações
-- próprias dá para liberar o editor sem liberar a IA (ou só a triagem da fila).
--
--   PAGINA:AI_GERAR     sessões, geração/ajuste de propostas, importação de documentos
--   PAGINA:AI_APLICAR   levar a proposta para uma página (ainda exige CRIAR/EDITAR)
--   PAGINA:AI_PROPOSTA  triar a fila de propostas abertas a partir de PRs (Fase C)

INSERT INTO public.tb_permissao (id, funcionalidade_id, acao, codigo, descricao, ativo, created_at, updated_at, created_by, updated_by) VALUES
	('e0c3ecc9-5e42-4e0c-a326-d514a4494728', '00000000-0000-0000-0200-000000000016', 'AI_GERAR', 'PAGINA:AI_GERAR',
	 'Usar o assistente de IA: sessões, geração de propostas e importação de documentos.',
	 true, now(), now(), 'seed', 'seed'),
	('0ed903ba-c43b-443d-b786-f9602d7d42bd', '00000000-0000-0000-0200-000000000016', 'AI_APLICAR', 'PAGINA:AI_APLICAR',
	 'Aplicar propostas da IA em páginas (cria ou altera rascunho; exige também criar/editar páginas).',
	 true, now(), now(), 'seed', 'seed'),
	('b4663d69-718c-4cea-818c-111f27ac2de3', '00000000-0000-0000-0200-000000000016', 'AI_PROPOSTA', 'PAGINA:AI_PROPOSTA',
	 'Triar a fila de propostas da IA abertas a partir de pull requests (aceitar ou rejeitar).',
	 true, now(), now(), 'seed', 'seed');

-- Ninguém perde acesso: todo grupo que hoje usa o assistente (via PAGINA:CRIAR)
-- recebe as três ações, inclusive grupos criados depois do seed.
INSERT INTO public.tb_grupo_permissao (grupo_id, permissao_id)
SELECT gp.grupo_id, nova.id
FROM public.tb_grupo_permissao gp
JOIN public.tb_permissao criar ON criar.id = gp.permissao_id AND criar.codigo = 'PAGINA:CRIAR'
CROSS JOIN public.tb_permissao nova
WHERE nova.codigo IN ('PAGINA:AI_GERAR', 'PAGINA:AI_APLICAR', 'PAGINA:AI_PROPOSTA')
ON CONFLICT DO NOTHING;
