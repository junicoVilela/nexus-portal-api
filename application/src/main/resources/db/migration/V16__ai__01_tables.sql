-- Nexus AI — tables (sessão + mensagens; jobs/propostas em S2)

CREATE TABLE public.tb_ai_sessao (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    objetivo character varying(40) NOT NULL,
    status character varying(40) NOT NULL,
    projeto_id uuid,
    modulo_id uuid,
    cliente_id uuid,
    pagina_id uuid,
    template_id uuid,
    briefing text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT pk_tb_ai_sessao PRIMARY KEY (id),
    CONSTRAINT ck_tb_ai_sessao_objetivo CHECK (
        (objetivo)::text = ANY ((ARRAY[
            'CRIAR_PAGINA'::character varying,
            'ATUALIZAR_PAGINA'::character varying
        ])::text[])
    ),
    CONSTRAINT ck_tb_ai_sessao_status CHECK (
        (status)::text = ANY ((ARRAY[
            'ABERTA'::character varying,
            'AGUARDANDO_USUARIO'::character varying,
            'PRONTA_PARA_GERAR'::character varying,
            'GERANDO'::character varying,
            'PRONTA'::character varying,
            'APLICADA'::character varying,
            'CANCELADA'::character varying,
            'ERRO'::character varying
        ])::text[])
    )
);

CREATE TABLE public.tb_ai_mensagem (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sessao_id uuid NOT NULL,
    papel character varying(20) NOT NULL,
    conteudo text NOT NULL,
    payload_json jsonb,
    ordem integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pk_tb_ai_mensagem PRIMARY KEY (id),
    CONSTRAINT fk_tb_ai_mensagem_sessao FOREIGN KEY (sessao_id)
        REFERENCES public.tb_ai_sessao (id) ON DELETE CASCADE,
    CONSTRAINT ck_tb_ai_mensagem_papel CHECK (
        (papel)::text = ANY ((ARRAY[
            'USUARIO'::character varying,
            'ASSISTENTE'::character varying,
            'SISTEMA'::character varying
        ])::text[])
    ),
    CONSTRAINT ck_tb_ai_mensagem_ordem CHECK (ordem >= 1)
);

CREATE INDEX ix_tb_ai_sessao_status ON public.tb_ai_sessao (status);
CREATE INDEX ix_tb_ai_sessao_created_at ON public.tb_ai_sessao (created_at DESC);
CREATE INDEX ix_tb_ai_mensagem_sessao_ordem ON public.tb_ai_mensagem (sessao_id, ordem);
