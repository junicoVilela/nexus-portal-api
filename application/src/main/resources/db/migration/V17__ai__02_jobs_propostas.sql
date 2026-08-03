-- Nexus AI — jobs de geração + propostas de página

CREATE TABLE public.tb_ai_job (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sessao_id uuid NOT NULL,
    tipo character varying(40) NOT NULL,
    status character varying(40) NOT NULL,
    erro_mensagem text,
    tokens_entrada integer,
    tokens_saida integer,
    modelo character varying(120),
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pk_tb_ai_job PRIMARY KEY (id),
    CONSTRAINT fk_tb_ai_job_sessao FOREIGN KEY (sessao_id)
        REFERENCES public.tb_ai_sessao (id) ON DELETE CASCADE,
    CONSTRAINT ck_tb_ai_job_tipo CHECK (
        (tipo)::text = ANY ((ARRAY[
            'TRIAGEM'::character varying,
            'GERAR_RASCUNHO'::character varying,
            'AJUSTAR'::character varying
        ])::text[])
    ),
    CONSTRAINT ck_tb_ai_job_status CHECK (
        (status)::text = ANY ((ARRAY[
            'PENDENTE'::character varying,
            'PROCESSANDO'::character varying,
            'SUCESSO'::character varying,
            'ERRO'::character varying
        ])::text[])
    )
);

CREATE TABLE public.tb_ai_proposta (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sessao_id uuid NOT NULL,
    job_id uuid NOT NULL,
    tipo character varying(40) NOT NULL,
    titulo character varying(200) NOT NULL,
    slug character varying(200) NOT NULL,
    codigo_tela character varying(120) NOT NULL,
    resumo text,
    conteudo_html text NOT NULL,
    template_id uuid,
    template_versao integer,
    qualidade_json jsonb,
    status character varying(40) NOT NULL,
    pagina_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pk_tb_ai_proposta PRIMARY KEY (id),
    CONSTRAINT fk_tb_ai_proposta_sessao FOREIGN KEY (sessao_id)
        REFERENCES public.tb_ai_sessao (id) ON DELETE CASCADE,
    CONSTRAINT fk_tb_ai_proposta_job FOREIGN KEY (job_id)
        REFERENCES public.tb_ai_job (id) ON DELETE CASCADE,
    CONSTRAINT ck_tb_ai_proposta_tipo CHECK (
        (tipo)::text = ANY ((ARRAY[
            'NOVA'::character varying,
            'ATUALIZACAO'::character varying
        ])::text[])
    ),
    CONSTRAINT ck_tb_ai_proposta_status CHECK (
        (status)::text = ANY ((ARRAY[
            'PENDENTE'::character varying,
            'ACEITA'::character varying,
            'REJEITADA'::character varying,
            'DESCARTADA'::character varying
        ])::text[])
    )
);

CREATE INDEX ix_tb_ai_job_sessao_status ON public.tb_ai_job (sessao_id, status);
CREATE INDEX ix_tb_ai_proposta_sessao_status ON public.tb_ai_proposta (sessao_id, status);
CREATE UNIQUE INDEX ux_tb_ai_proposta_job ON public.tb_ai_proposta (job_id);
