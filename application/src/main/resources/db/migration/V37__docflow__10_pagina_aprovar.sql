-- Ação PAGINA:APROVAR no catálogo RBAC.
--
-- Aprovar e devolver estavam sob PAGINA:EDITAR, que também dá poder de alterar
-- o conteúdo. O grupo REVISOR (só :LER) não conseguia aprovar nada — o que
-- inviabiliza a regra de que o responsável pela revisão é quem aprova.
--
-- Com a ação própria, revisor aprova/devolve sem poder editar a página.

INSERT INTO public.tb_permissao (id, funcionalidade_id, acao, codigo, descricao, ativo, created_at, updated_at, created_by, updated_by) VALUES
	('6f2a1c53-9d0b-4f2e-8a71-2c6f5b0e93d4', '00000000-0000-0000-0200-000000000016', 'APROVAR', 'PAGINA:APROVAR',
	 'Aprovar ou devolver páginas em revisão, sem permitir a edição do conteúdo.',
	 true, now(), now(), 'seed', 'seed');

-- ADMIN e EDITOR já revisavam via PAGINA:EDITAR; REVISOR passa a poder aprovar.
INSERT INTO public.tb_grupo_permissao (grupo_id, permissao_id) VALUES
	('00000000-0000-0000-0000-000000000801', '6f2a1c53-9d0b-4f2e-8a71-2c6f5b0e93d4'),
	('00000000-0000-0000-0000-000000000802', '6f2a1c53-9d0b-4f2e-8a71-2c6f5b0e93d4'),
	('00000000-0000-0000-0000-000000000804', '6f2a1c53-9d0b-4f2e-8a71-2c6f5b0e93d4');

-- O revisor precisa enxergar a fila e o histórico editorial para decidir.
-- REVISOR já possui PAGINA:LER pelo seed do catálogo (V5).
