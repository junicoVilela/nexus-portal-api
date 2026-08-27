-- Release Orchestrator — instalação do produto por cliente (RF-003 / RF-006 / RF-007)
-- Tipo de implantação RPA: Docker pull, Docker .tar, Linux manual, Windows manual.
-- Deploy futuro aponta para instalacao_id, nunca para host_id.

CREATE TABLE public.tb_instalacao_cliente (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    codigo character varying(40) NOT NULL,
    nome character varying(200) NOT NULL,
    cliente_id uuid NOT NULL,
    host_id uuid NOT NULL,
    produto_id uuid NOT NULL,
    tipo_implantacao character varying(30) NOT NULL,
    status character varying(20) DEFAULT 'INEXISTENTE'::character varying NOT NULL,
    ambiente character varying(20) NOT NULL,
    imagem_ref character varying(300),
    arquivo_imagem_ref character varying(400),
    diretorio_instalacao character varying(400),
    observacoes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_instalacao_cliente_tipo CHECK (((tipo_implantacao)::text = ANY ((ARRAY['DOCKER_PULL'::character varying, 'DOCKER_TAR'::character varying, 'LINUX_MANUAL'::character varying, 'WINDOWS_MANUAL'::character varying])::text[]))),
    CONSTRAINT ck_tb_instalacao_cliente_status CHECK (((status)::text = ANY ((ARRAY['INEXISTENTE'::character varying, 'ATIVA'::character varying, 'INATIVA'::character varying])::text[]))),
    CONSTRAINT ck_tb_instalacao_cliente_ambiente CHECK (((ambiente)::text = ANY ((ARRAY['PROD'::character varying, 'HOM'::character varying, 'DEV'::character varying, 'TEST'::character varying])::text[])))
);

ALTER TABLE ONLY public.tb_instalacao_cliente
    ADD CONSTRAINT pk_tb_instalacao_cliente PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_instalacao_cliente
    ADD CONSTRAINT uq_tb_instalacao_cliente_codigo UNIQUE (codigo);

ALTER TABLE ONLY public.tb_instalacao_cliente
    ADD CONSTRAINT uq_tb_instalacao_cliente_alvo UNIQUE (cliente_id, produto_id, host_id, ambiente);

ALTER TABLE ONLY public.tb_instalacao_cliente
    ADD CONSTRAINT fk_tb_instalacao_cliente_cliente FOREIGN KEY (cliente_id)
        REFERENCES public.tb_cliente_orchestrator(id);

ALTER TABLE ONLY public.tb_instalacao_cliente
    ADD CONSTRAINT fk_tb_instalacao_cliente_host FOREIGN KEY (host_id)
        REFERENCES public.tb_host_orchestrator(id);

ALTER TABLE ONLY public.tb_instalacao_cliente
    ADD CONSTRAINT fk_tb_instalacao_cliente_produto FOREIGN KEY (produto_id)
        REFERENCES public.tb_produto_rh(id);

CREATE INDEX idx_tb_instalacao_cliente_cliente ON public.tb_instalacao_cliente USING btree (cliente_id);

CREATE INDEX idx_tb_instalacao_cliente_host ON public.tb_instalacao_cliente USING btree (host_id);

CREATE INDEX idx_tb_instalacao_cliente_produto ON public.tb_instalacao_cliente USING btree (produto_id);

CREATE INDEX idx_tb_instalacao_cliente_tipo ON public.tb_instalacao_cliente USING btree (tipo_implantacao);

CREATE INDEX idx_tb_instalacao_cliente_status ON public.tb_instalacao_cliente USING btree (status);

CREATE INDEX idx_tb_instalacao_cliente_nome ON public.tb_instalacao_cliente USING btree (lower((nome)::text));

COMMENT ON TABLE public.tb_instalacao_cliente IS 'Instalação do produto (RPA) em um host, por cliente. Alvo de deploy(releaseId, instalacaoId).';

COMMENT ON COLUMN public.tb_instalacao_cliente.tipo_implantacao IS 'DOCKER_PULL (baixa imagem), DOCKER_TAR (imagem .tar), LINUX_MANUAL, WINDOWS_MANUAL.';

COMMENT ON COLUMN public.tb_instalacao_cliente.status IS 'INEXISTENTE = cadastrada antes de existir no host (RF-007). ATIVA/INATIVA = inventário operacional (RF-006).';

COMMENT ON COLUMN public.tb_instalacao_cliente.imagem_ref IS 'Referência da imagem no registry quando tipo = DOCKER_PULL (ex.: nexus/rpa:1.4.0).';

COMMENT ON COLUMN public.tb_instalacao_cliente.arquivo_imagem_ref IS 'Caminho ou nome do arquivo .tar quando tipo = DOCKER_TAR.';

COMMENT ON COLUMN public.tb_instalacao_cliente.diretorio_instalacao IS 'Diretório no host quando tipo = LINUX_MANUAL ou WINDOWS_MANUAL.';

-- RBAC: funcionalidade INSTALACAO no domínio RELEASE_ORCHESTRATOR
INSERT INTO public.tb_funcionalidade (id, dominio_id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by) VALUES
	('00000000-0000-0000-0200-000000000027', '00000000-0000-0000-0100-000000000004', 'INSTALACAO', 'Instalação', 'Instalações do produto por cliente e host (tipos de implantação RPA).', true, now(), now(), 'seed', 'seed');

INSERT INTO public.tb_permissao (id, funcionalidade_id, acao, codigo, descricao, ativo, created_at, updated_at, created_by, updated_by) VALUES
	('f9370d29-4893-4cb4-ab23-bff3b8f64eb4', '00000000-0000-0000-0200-000000000027', 'LER', 'INSTALACAO:LER', NULL, true, now(), now(), 'seed', 'seed'),
	('38d296b6-a3a9-4467-ac30-1d6434543eef', '00000000-0000-0000-0200-000000000027', 'CRIAR', 'INSTALACAO:CRIAR', NULL, true, now(), now(), 'seed', 'seed'),
	('fc5b3cff-8525-42d2-b057-a7be80c3d9bc', '00000000-0000-0000-0200-000000000027', 'EDITAR', 'INSTALACAO:EDITAR', NULL, true, now(), now(), 'seed', 'seed'),
	('b2e8b1e5-713f-40fb-945a-31b9eb3b4a24', '00000000-0000-0000-0200-000000000027', 'EXCLUIR', 'INSTALACAO:EXCLUIR', NULL, true, now(), now(), 'seed', 'seed');

-- ADMIN: CRUD | EDITOR: CRUD | LEITOR/REVISOR: LER
INSERT INTO public.tb_grupo_permissao (grupo_id, permissao_id) VALUES
	('00000000-0000-0000-0000-000000000801', 'f9370d29-4893-4cb4-ab23-bff3b8f64eb4'),
	('00000000-0000-0000-0000-000000000801', '38d296b6-a3a9-4467-ac30-1d6434543eef'),
	('00000000-0000-0000-0000-000000000801', 'fc5b3cff-8525-42d2-b057-a7be80c3d9bc'),
	('00000000-0000-0000-0000-000000000801', 'b2e8b1e5-713f-40fb-945a-31b9eb3b4a24'),
	('00000000-0000-0000-0000-000000000802', 'f9370d29-4893-4cb4-ab23-bff3b8f64eb4'),
	('00000000-0000-0000-0000-000000000802', '38d296b6-a3a9-4467-ac30-1d6434543eef'),
	('00000000-0000-0000-0000-000000000802', 'fc5b3cff-8525-42d2-b057-a7be80c3d9bc'),
	('00000000-0000-0000-0000-000000000802', 'b2e8b1e5-713f-40fb-945a-31b9eb3b4a24'),
	('00000000-0000-0000-0000-000000000803', 'f9370d29-4893-4cb4-ab23-bff3b8f64eb4'),
	('00000000-0000-0000-0000-000000000804', 'f9370d29-4893-4cb4-ab23-bff3b8f64eb4');
