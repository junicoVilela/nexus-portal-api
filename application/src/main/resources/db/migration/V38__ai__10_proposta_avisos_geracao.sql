ALTER TABLE public.tb_ai_proposta
  ADD COLUMN avisos_geracao JSONB NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN public.tb_ai_proposta.avisos_geracao IS
  'Avisos quando a geração caiu em fallback (resposta inválida da IA, provider de demonstração, código de tela ausente).';
