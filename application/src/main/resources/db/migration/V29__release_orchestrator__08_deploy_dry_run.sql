-- Manifesto por release+tipo e histórico de deploy(releaseId, instalacaoId).
-- Adaptadores reais virão depois; nesta versão o modo é sempre DRY_RUN.

CREATE TABLE public.tb_manifesto_implantacao (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    release_id uuid NOT NULL,
    tipo_implantacao character varying(30) NOT NULL,
    imagem_ref character varying(300),
    arquivo_imagem_ref character varying(400),
    diretorio_instalacao character varying(400),
    observacoes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_manifesto_implantacao_tipo CHECK (((tipo_implantacao)::text = ANY ((ARRAY['DOCKER_PULL'::character varying, 'DOCKER_TAR'::character varying, 'LINUX_MANUAL'::character varying, 'WINDOWS_MANUAL'::character varying])::text[])))
);

ALTER TABLE ONLY public.tb_manifesto_implantacao
    ADD CONSTRAINT pk_tb_manifesto_implantacao PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_manifesto_implantacao
    ADD CONSTRAINT uq_tb_manifesto_implantacao UNIQUE (release_id, tipo_implantacao);

ALTER TABLE ONLY public.tb_manifesto_implantacao
    ADD CONSTRAINT fk_tb_manifesto_implantacao_release FOREIGN KEY (release_id)
        REFERENCES public.tb_release(id) ON DELETE CASCADE;

CREATE INDEX idx_tb_manifesto_implantacao_release ON public.tb_manifesto_implantacao USING btree (release_id);

COMMENT ON TABLE public.tb_manifesto_implantacao IS 'O que cada tipo de implantação aplica nesta release. Sobrescreve o derivado da instalação.';

CREATE TABLE public.tb_deploy_instalacao (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    release_id uuid NOT NULL,
    instalacao_id uuid NOT NULL,
    entrega_id uuid,
    tipo_implantacao character varying(30) NOT NULL,
    operacao character varying(20) NOT NULL,
    modo character varying(20) DEFAULT 'DRY_RUN'::character varying NOT NULL,
    status character varying(20) NOT NULL,
    versao_origem character varying(80),
    versao_destino character varying(80) NOT NULL,
    imagem_ref character varying(300),
    arquivo_imagem_ref character varying(400),
    diretorio_instalacao character varying(400),
    fingerprint character varying(200) NOT NULL,
    mensagem text,
    erro text,
    operador character varying(120),
    reutilizado boolean DEFAULT false NOT NULL,
    iniciado_em timestamp with time zone,
    concluido_em timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_deploy_instalacao_tipo CHECK (((tipo_implantacao)::text = ANY ((ARRAY['DOCKER_PULL'::character varying, 'DOCKER_TAR'::character varying, 'LINUX_MANUAL'::character varying, 'WINDOWS_MANUAL'::character varying])::text[]))),
    CONSTRAINT ck_tb_deploy_instalacao_operacao CHECK (((operacao)::text = ANY ((ARRAY['CRIAR'::character varying, 'ATUALIZAR'::character varying])::text[]))),
    CONSTRAINT ck_tb_deploy_instalacao_modo CHECK (((modo)::text = ANY ((ARRAY['DRY_RUN'::character varying, 'REAL'::character varying])::text[]))),
    CONSTRAINT ck_tb_deploy_instalacao_status CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'EM_ANDAMENTO'::character varying, 'CONCLUIDO'::character varying, 'FALHA'::character varying, 'IGNORADO'::character varying])::text[])))
);

ALTER TABLE ONLY public.tb_deploy_instalacao
    ADD CONSTRAINT pk_tb_deploy_instalacao PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_deploy_instalacao
    ADD CONSTRAINT fk_tb_deploy_instalacao_release FOREIGN KEY (release_id)
        REFERENCES public.tb_release(id);

ALTER TABLE ONLY public.tb_deploy_instalacao
    ADD CONSTRAINT fk_tb_deploy_instalacao_instalacao FOREIGN KEY (instalacao_id)
        REFERENCES public.tb_instalacao_cliente(id);

ALTER TABLE ONLY public.tb_deploy_instalacao
    ADD CONSTRAINT fk_tb_deploy_instalacao_entrega FOREIGN KEY (entrega_id)
        REFERENCES public.tb_entrega(id);

CREATE INDEX idx_tb_deploy_instalacao_instalacao ON public.tb_deploy_instalacao USING btree (instalacao_id, created_at DESC);

CREATE INDEX idx_tb_deploy_instalacao_release ON public.tb_deploy_instalacao USING btree (release_id);

CREATE INDEX idx_tb_deploy_instalacao_entrega ON public.tb_deploy_instalacao USING btree (entrega_id);

CREATE INDEX idx_tb_deploy_instalacao_fingerprint ON public.tb_deploy_instalacao USING btree (release_id, instalacao_id, fingerprint);

COMMENT ON TABLE public.tb_deploy_instalacao IS 'Histórico de deploy(releaseId, instalacaoId). Dry-run não altera o host.';
