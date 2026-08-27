-- Release Orchestrator — configuração da instalação (RF-004) e reserva de portas (RF-005)
-- Config é por instalação (não confundir com tb_config_entrega_orchestrator).
-- Portas pertencem a host + instalação. UNIQUE ativo: (host, porta, protocolo).

CREATE TABLE public.tb_configuracao_instalacao (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    instalacao_id uuid NOT NULL,
    tipo_banco character varying(20),
    banco_host character varying(200),
    banco_porta integer,
    banco_nome character varying(120),
    banco_usuario character varying(120),
    banco_credencial_ref character varying(200),
    url_backend character varying(500),
    url_frontend character varying(500),
    parametros text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_configuracao_instalacao_tipo_banco CHECK (((tipo_banco IS NULL) OR ((tipo_banco)::text = ANY ((ARRAY['ORACLE'::character varying, 'SQLSERVER'::character varying, 'POSTGRES'::character varying])::text[])))),
    CONSTRAINT ck_tb_configuracao_instalacao_banco_porta CHECK (((banco_porta IS NULL) OR ((banco_porta >= 1) AND (banco_porta <= 65535))))
);

ALTER TABLE ONLY public.tb_configuracao_instalacao
    ADD CONSTRAINT pk_tb_configuracao_instalacao PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_configuracao_instalacao
    ADD CONSTRAINT uq_tb_configuracao_instalacao_instalacao UNIQUE (instalacao_id);

ALTER TABLE ONLY public.tb_configuracao_instalacao
    ADD CONSTRAINT fk_tb_configuracao_instalacao_instalacao FOREIGN KEY (instalacao_id)
        REFERENCES public.tb_instalacao_cliente(id) ON DELETE CASCADE;

CREATE TABLE public.tb_reserva_porta (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    host_id uuid NOT NULL,
    instalacao_id uuid NOT NULL,
    tipo character varying(30) NOT NULL,
    papel character varying(20) NOT NULL,
    porta integer NOT NULL,
    protocolo character varying(10) DEFAULT 'TCP'::character varying NOT NULL,
    status character varying(20) DEFAULT 'RESERVADA'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_reserva_porta_tipo CHECK (((tipo)::text = ANY ((ARRAY['HTTP'::character varying, 'HTTPS'::character varying, 'AJP'::character varying, 'TOMCAT_SHUTDOWN'::character varying, 'DEBUG'::character varying, 'JMX'::character varying, 'BANCO'::character varying, 'OUTRO'::character varying])::text[]))),
    CONSTRAINT ck_tb_reserva_porta_papel CHECK (((papel)::text = ANY ((ARRAY['BACKEND'::character varying, 'FRONTEND'::character varying, 'OUTRO'::character varying])::text[]))),
    CONSTRAINT ck_tb_reserva_porta_protocolo CHECK (((protocolo)::text = ANY ((ARRAY['TCP'::character varying, 'UDP'::character varying])::text[]))),
    CONSTRAINT ck_tb_reserva_porta_status CHECK (((status)::text = ANY ((ARRAY['DISPONIVEL'::character varying, 'RESERVADA'::character varying, 'EM_USO'::character varying, 'LIBERADA'::character varying, 'BLOQUEADA'::character varying])::text[]))),
    CONSTRAINT ck_tb_reserva_porta_porta CHECK (((porta >= 1) AND (porta <= 65535)))
);

ALTER TABLE ONLY public.tb_reserva_porta
    ADD CONSTRAINT pk_tb_reserva_porta PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_reserva_porta
    ADD CONSTRAINT fk_tb_reserva_porta_host FOREIGN KEY (host_id)
        REFERENCES public.tb_host_orchestrator(id);

ALTER TABLE ONLY public.tb_reserva_porta
    ADD CONSTRAINT fk_tb_reserva_porta_instalacao FOREIGN KEY (instalacao_id)
        REFERENCES public.tb_instalacao_cliente(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uq_tb_reserva_porta_ativa
    ON public.tb_reserva_porta USING btree (host_id, porta, protocolo)
    WHERE ((status)::text = ANY ((ARRAY['RESERVADA'::character varying, 'EM_USO'::character varying, 'BLOQUEADA'::character varying])::text[]));

CREATE INDEX idx_tb_reserva_porta_host ON public.tb_reserva_porta USING btree (host_id);

CREATE INDEX idx_tb_reserva_porta_instalacao ON public.tb_reserva_porta USING btree (instalacao_id);

COMMENT ON TABLE public.tb_configuracao_instalacao IS 'Configuração específica da instalação: banco, URLs e parâmetros. Credencial só por referência.';

COMMENT ON COLUMN public.tb_configuracao_instalacao.banco_credencial_ref IS 'Referência a segredo. Nunca armazena senha de banco em texto puro.';

COMMENT ON TABLE public.tb_reserva_porta IS 'Portas da instalação no host. Conflito: mesmo host + porta + protocolo em status ocupado.';
