-- Fase B do assistente: ajuste de página existente por patch (docs/doc-flow/13-assistente-ia-fase-b.md).
ALTER TABLE public.tb_ai_sessao
  ADD COLUMN version_base BIGINT,
  ADD COLUMN secao_id VARCHAR(20);

COMMENT ON COLUMN public.tb_ai_sessao.version_base IS
  'Versão (@Version) da página quando o ajuste foi pedido; o patch só é aplicado sobre ela.';
COMMENT ON COLUMN public.tb_ai_sessao.secao_id IS
  'Seção do esboço (s1, s2…) que limita o ajuste; nulo = página inteira.';

ALTER TABLE public.tb_ai_proposta
  ADD COLUMN patch_json JSONB,
  ADD COLUMN operacoes_aceitas JSONB;

COMMENT ON COLUMN public.tb_ai_proposta.patch_json IS
  'Operações propostas para a página (com texto anterior), quando tipo = ATUALIZACAO.';
COMMENT ON COLUMN public.tb_ai_proposta.operacoes_aceitas IS
  'Ids das operações aplicadas pelo autor; base da métrica de aceite parcial.';
