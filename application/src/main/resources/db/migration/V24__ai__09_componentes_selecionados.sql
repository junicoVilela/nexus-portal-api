ALTER TABLE public.tb_ai_sessao
  ADD COLUMN componentes_selecionados JSONB NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN public.tb_ai_sessao.componentes_selecionados IS
  'IDs dos componentes do catálogo DocFlow aprovados pelo usuário para a geração da página.';
