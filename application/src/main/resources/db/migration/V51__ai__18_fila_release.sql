-- INT-301/303: a fila de propostas também recebe releases publicadas. Cada tela citada na
-- release vira um item PARA_REVISAR (a IA só gera o ajuste quando alguém pede).

ALTER TABLE public.tb_ai_pr_evento
  ADD COLUMN origem character varying(20) DEFAULT 'PR' NOT NULL,
  ADD COLUMN release_id uuid,
  ALTER COLUMN delivery_id DROP NOT NULL,
  ALTER COLUMN numero_pr DROP NOT NULL;

ALTER TABLE public.tb_ai_pr_evento DROP CONSTRAINT uk_tb_ai_pr_evento_pr;
CREATE UNIQUE INDEX uk_tb_ai_pr_evento_pr ON public.tb_ai_pr_evento (repositorio, numero_pr) WHERE origem = 'PR';
CREATE UNIQUE INDEX uk_tb_ai_pr_evento_release_tela ON public.tb_ai_pr_evento (release_id, codigo_tela) WHERE origem = 'RELEASE';

ALTER TABLE public.tb_ai_pr_evento DROP CONSTRAINT ck_tb_ai_pr_evento_status;
ALTER TABLE public.tb_ai_pr_evento ADD CONSTRAINT ck_tb_ai_pr_evento_status CHECK (status IN (
    'RECEBIDO', 'IGNORADO', 'AGUARDANDO_RASCUNHO', 'EM_FILA', 'ERRO', 'PARA_REVISAR'));
ALTER TABLE public.tb_ai_pr_evento ADD CONSTRAINT ck_tb_ai_pr_evento_origem CHECK (origem IN ('PR', 'RELEASE'));

COMMENT ON COLUMN public.tb_ai_pr_evento.origem IS 'PR (webhook GitHub) ou RELEASE (Release Orchestrator).';
