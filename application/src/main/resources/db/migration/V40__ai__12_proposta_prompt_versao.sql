ALTER TABLE public.tb_ai_proposta
  ADD COLUMN prompt_versao VARCHAR(120);

COMMENT ON COLUMN public.tb_ai_proposta.prompt_versao IS
  'Prompt que gerou a proposta (ex.: gerar-page-spec@2.2, ver ai/src/main/resources/prompts); base para comparar aceite entre versões.';
