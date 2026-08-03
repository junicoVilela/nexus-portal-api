-- Identity Access — seed usuários e política de senha

INSERT INTO public.tb_politica_senha (id, tamanho_minimo, exigir_maiuscula, exigir_minuscula, exigir_numero, exigir_especial, expira_senha_dias, quantidade_historico, max_tentativas_invalidas, ativo, created_at, updated_at, created_by, updated_by) VALUES
	('00000000-0000-0000-0000-000000000901', 8, true, true, true, false, NULL, 3, 5, true, '2026-08-03 01:37:36.785134+00', '2026-08-03 01:37:36.785134+00', 'seed', 'seed');

INSERT INTO public.tb_usuario (id, username, password, nome, email, ativo, created_at, updated_at, created_by, updated_by, bloqueado, tentativas_invalidas, trocar_senha_proximo_login) VALUES
	('00000000-0000-0000-0000-000000000701', 'admin', '$2b$10$hzUCFIZ3npCft.La1cPYB.W2qGRIBGYOsN5ndGlsXbJtJ1L7Ma4Ei', 'Administrador', NULL, true, '2026-08-03 01:37:36.686159+00', '2026-08-03 01:37:36.686159+00', 'seed', 'seed', false, 0, false),
	('00000000-0000-0000-0000-000000000702', 'editor', '$2b$10$p22zwg4H9Iu.0V3CVX//y.IVPSjrZWgZfdW0giT2J8oHAKYeNxA1a', 'Editor Padrão', NULL, true, '2026-08-03 01:37:36.686159+00', '2026-08-03 01:37:36.686159+00', 'seed', 'seed', false, 0, false),
	('00000000-0000-0000-0000-000000000703', 'revisor', '$2b$10$XQVyjh5JWjR2mK.xiOJo7uw8Jv.TpkzG68/pIM9TEnUxCxYuWq6a6', 'Revisor de Conteúdo', NULL, true, '2026-08-03 01:37:36.686159+00', '2026-08-03 01:37:36.686159+00', 'seed', 'seed', false, 0, false),
	('00000000-0000-0000-0000-000000000704', 'leitor', '$2b$10$tMgFDU5L4cWEEKScZjomT.l8sPckl6SgAOP9p2OkmSnLSggSWAWzi', 'Leitor Padrão', NULL, true, '2026-08-03 01:37:36.686159+00', '2026-08-03 01:37:36.686159+00', 'seed', 'seed', false, 0, false);

INSERT INTO public.tb_grupo_usuario (grupo_id, usuario_id) VALUES
	('00000000-0000-0000-0000-000000000801', '00000000-0000-0000-0000-000000000701'),
	('00000000-0000-0000-0000-000000000802', '00000000-0000-0000-0000-000000000702'),
	('00000000-0000-0000-0000-000000000804', '00000000-0000-0000-0000-000000000703'),
	('00000000-0000-0000-0000-000000000803', '00000000-0000-0000-0000-000000000704');
