-- Fase C (AI-601…612): PR mergeado no GitHub vira proposta na fila editorial.
--
-- Cada entrega do webhook é gravada uma vez (delivery_id único, AI-603) e cada PR
-- gera no máximo um item (repositorio + numero_pr único): reentregas e retries do
-- GitHub não duplicam propostas.

CREATE TABLE public.tb_ai_pr_evento (
    id uuid NOT NULL,
    delivery_id character varying(80) NOT NULL,
    repositorio character varying(200) NOT NULL,
    numero_pr integer NOT NULL,
    titulo character varying(500) NOT NULL,
    corpo text,
    rotulos jsonb DEFAULT '[]'::jsonb NOT NULL,
    url character varying(500) NOT NULL,
    autor character varying(120),
    branch_base character varying(200) NOT NULL,
    merge_sha character varying(64),
    merged_at timestamp with time zone,
    classificacao character varying(30),
    codigo_tela character varying(120),
    status character varying(30) NOT NULL,
    mensagem character varying(500),
    sessao_id uuid,
    pagina_id uuid,
    responsavel character varying(120),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pk_tb_ai_pr_evento PRIMARY KEY (id),
    CONSTRAINT uk_tb_ai_pr_evento_delivery UNIQUE (delivery_id),
    CONSTRAINT uk_tb_ai_pr_evento_pr UNIQUE (repositorio, numero_pr),
    CONSTRAINT fk_tb_ai_pr_evento_sessao FOREIGN KEY (sessao_id) REFERENCES public.tb_ai_sessao(id),
    CONSTRAINT ck_tb_ai_pr_evento_classificacao CHECK (classificacao IS NULL OR classificacao IN (
        'UI_NOVA', 'UI_ALTERACAO', 'SO_BACKEND', 'IRRELEVANTE')),
    CONSTRAINT ck_tb_ai_pr_evento_status CHECK (status IN (
        'RECEBIDO', 'IGNORADO', 'AGUARDANDO_RASCUNHO', 'EM_FILA', 'ERRO'))
);

CREATE INDEX ix_tb_ai_pr_evento_status_created ON public.tb_ai_pr_evento (status, created_at DESC);

COMMENT ON TABLE public.tb_ai_pr_evento IS
  'PR mergeado recebido pelo webhook GitHub e o que a IA fez com ele (fila de propostas).';
COMMENT ON COLUMN public.tb_ai_pr_evento.delivery_id IS 'Header X-GitHub-Delivery (idempotência).';
COMMENT ON COLUMN public.tb_ai_pr_evento.codigo_tela IS 'Código de tela citado no PR, usado para achar a página.';
COMMENT ON COLUMN public.tb_ai_pr_evento.pagina_id IS 'Página existente que o PR altera; nulo = página nova.';
COMMENT ON COLUMN public.tb_ai_pr_evento.responsavel IS 'Quem assumiu o item na fila.';

-- Sessões abertas pelo webhook nascem do usuário "system"; quem assume o item na
-- fila passa a ser o responsável e usa os fluxos normais do assistente.
ALTER TABLE public.tb_ai_sessao
  ADD COLUMN responsavel character varying(120);

COMMENT ON COLUMN public.tb_ai_sessao.responsavel IS
  'Dono atual da sessão quando difere de created_by (item da fila de PR assumido).';
