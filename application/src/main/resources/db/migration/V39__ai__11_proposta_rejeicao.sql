ALTER TABLE public.tb_ai_proposta
  ADD COLUMN motivo_rejeicao VARCHAR(500);

COMMENT ON COLUMN public.tb_ai_proposta.motivo_rejeicao IS
  'Motivo opcional informado pelo autor ao rejeitar a proposta (base para ajuste de prompts).';
