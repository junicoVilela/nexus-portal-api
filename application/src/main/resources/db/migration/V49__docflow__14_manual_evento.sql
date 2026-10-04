-- Onda F (INT-605): o manual hospedado (Onda D) registra buscas e páginas abertas. Sem
-- identificar o leitor (nada de usuário, IP ou chave): só o termo, o resultado e a tela.
-- Retenção: a mesma dos eventos da ajuda (docflow.ajuda.retencao-eventos-dias).

CREATE TABLE public.tb_manual_evento (
    id uuid NOT NULL,
    cliente_id uuid NOT NULL,
    publicacao_id uuid,
    tipo character varying(30) NOT NULL,
    termo character varying(200),
    codigo_tela character varying(120),
    resultados integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pk_tb_manual_evento PRIMARY KEY (id),
    CONSTRAINT ck_tb_manual_evento_tipo CHECK (tipo IN ('BUSCA', 'BUSCA_SEM_RESULTADO', 'PAGINA_ABERTA'))
);

CREATE INDEX ix_tb_manual_evento_created ON public.tb_manual_evento (created_at DESC);

COMMENT ON TABLE public.tb_manual_evento IS
  'Buscas e páginas abertas no manual hospedado, sem identificar o leitor (lacunas do manual).';
