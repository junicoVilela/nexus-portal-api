-- Builds Jenkins disparados pela ficha da instalação, com cópia para artifacts/.

CREATE TABLE public.tb_build_instalacao_artefato (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    instalacao_id uuid NOT NULL,
    produto_id uuid NOT NULL,
    modulo_id uuid,
    alvo_id character varying(80) NOT NULL,
    jenkins_job character varying(200) NOT NULL,
    queue_url character varying(500),
    build_number integer,
    padrao_asset character varying(200),
    nome_arquivo character varying(200),
    status character varying(20) DEFAULT 'ENFILEIRADO'::character varying NOT NULL,
    mensagem text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_build_instalacao_artefato_status CHECK (((status)::text = ANY ((ARRAY['ENFILEIRADO'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying])::text[])))
);

ALTER TABLE ONLY public.tb_build_instalacao_artefato
    ADD CONSTRAINT pk_tb_build_instalacao_artefato PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_build_instalacao_artefato
    ADD CONSTRAINT fk_tb_build_instalacao_artefato_instalacao FOREIGN KEY (instalacao_id)
        REFERENCES public.tb_instalacao_cliente(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_build_instalacao_artefato
    ADD CONSTRAINT fk_tb_build_instalacao_artefato_produto FOREIGN KEY (produto_id)
        REFERENCES public.tb_produto_rh(id);

ALTER TABLE ONLY public.tb_build_instalacao_artefato
    ADD CONSTRAINT fk_tb_build_instalacao_artefato_modulo FOREIGN KEY (modulo_id)
        REFERENCES public.tb_modulo_produto(id) ON DELETE SET NULL;

CREATE INDEX idx_tb_build_instalacao_artefato_instalacao
    ON public.tb_build_instalacao_artefato USING btree (instalacao_id, created_at DESC);

COMMENT ON TABLE public.tb_build_instalacao_artefato IS
    'Acompanha jobs Jenkins disparados na instalação e a cópia para artifacts/.';
