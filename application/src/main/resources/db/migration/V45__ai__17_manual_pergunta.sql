-- Onda E (INT-506): cada pergunta ao manual publicado, para medir acerto e achar lacunas.
-- Não guarda quem perguntou (usuário, IP, token): só a pergunta, o resultado e as telas citadas.

CREATE TABLE public.tb_ai_manual_pergunta (
    id uuid NOT NULL,
    publicacao_id uuid NOT NULL,
    origem character varying(20) NOT NULL,
    pergunta character varying(500) NOT NULL,
    modo character varying(20) NOT NULL,
    codigos_citados jsonb DEFAULT '[]'::jsonb NOT NULL,
    melhor_cobertura numeric(4,3),
    latencia_ms bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pk_tb_ai_manual_pergunta PRIMARY KEY (id),
    CONSTRAINT ck_tb_ai_manual_pergunta_origem CHECK (origem IN ('PORTAL', 'LEITOR')),
    CONSTRAINT ck_tb_ai_manual_pergunta_modo CHECK (modo IN ('IA', 'TRECHOS', 'NAO_SEI'))
);

CREATE INDEX ix_tb_ai_manual_pergunta_created ON public.tb_ai_manual_pergunta (created_at DESC);

COMMENT ON TABLE public.tb_ai_manual_pergunta IS
  'Perguntas ao manual publicado (answer engine): resultado e telas citadas, sem identificar quem perguntou.';
COMMENT ON COLUMN public.tb_ai_manual_pergunta.modo IS
  'IA = resposta gerada com citação; TRECHOS = trechos sem IA (módulo desligado ou falha); NAO_SEI = sem base no manual.';
