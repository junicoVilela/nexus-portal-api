-- Categoria fechada da rejeição (AI-701): soma no painel de qualidade por versão
-- de prompt. O texto livre continua em motivo_rejeicao. Nulo nas rejeições
-- anteriores e quando o autor não escolhe.

ALTER TABLE public.tb_ai_proposta
  ADD COLUMN categoria_rejeicao VARCHAR(40);

ALTER TABLE public.tb_ai_proposta
  ADD CONSTRAINT ck_tb_ai_proposta_categoria_rejeicao CHECK (
    categoria_rejeicao IS NULL OR categoria_rejeicao IN (
      'CONTEUDO_INCORRETO', 'FALTOU_INFORMACAO', 'ESTRUTURA_INADEQUADA',
      'MODELO_ERRADO', 'LINGUAGEM', 'OUTRO'));

COMMENT ON COLUMN public.tb_ai_proposta.categoria_rejeicao IS
  'Categoria escolhida ao rejeitar (AiCategoriaRejeicao); base dos padrões de rejeição por prompt.';
