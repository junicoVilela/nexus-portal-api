-- Release Orchestrator — cadastro de hosts (RF-002, Fase 1 da automação de implantação)
-- Máquina de execução: Windows, Linux ou host com Docker. Sem senha em texto puro.

CREATE TABLE public.tb_host_orchestrator (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    codigo character varying(40) NOT NULL,
    nome character varying(200) NOT NULL,
    hostname character varying(255) NOT NULL,
    endereco_ip character varying(45),
    sistema_operacional character varying(20) NOT NULL,
    docker_disponivel boolean DEFAULT false NOT NULL,
    tipo_conexao character varying(20) NOT NULL,
    porta_conexao integer,
    usuario_conexao character varying(120),
    credencial_ref character varying(200),
    ativo boolean DEFAULT true NOT NULL,
    observacoes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_host_orchestrator_so CHECK (((sistema_operacional)::text = ANY ((ARRAY['WINDOWS'::character varying, 'LINUX'::character varying])::text[]))),
    CONSTRAINT ck_tb_host_orchestrator_tipo_conexao CHECK (((tipo_conexao)::text = ANY ((ARRAY['SSH'::character varying, 'WINRM'::character varying, 'DOCKER'::character varying])::text[]))),
    CONSTRAINT ck_tb_host_orchestrator_porta CHECK (((porta_conexao IS NULL) OR ((porta_conexao >= 1) AND (porta_conexao <= 65535))))
);

ALTER TABLE ONLY public.tb_host_orchestrator
    ADD CONSTRAINT pk_tb_host_orchestrator PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_host_orchestrator
    ADD CONSTRAINT uq_tb_host_orchestrator_codigo UNIQUE (codigo);

CREATE UNIQUE INDEX uq_tb_host_orchestrator_hostname_ci
    ON public.tb_host_orchestrator USING btree (lower((hostname)::text));

CREATE INDEX idx_tb_host_orchestrator_ativo ON public.tb_host_orchestrator USING btree (ativo);

CREATE INDEX idx_tb_host_orchestrator_so ON public.tb_host_orchestrator USING btree (sistema_operacional);

CREATE INDEX idx_tb_host_orchestrator_docker ON public.tb_host_orchestrator USING btree (docker_disponivel);

CREATE INDEX idx_tb_host_orchestrator_nome ON public.tb_host_orchestrator USING btree (lower((nome)::text));

COMMENT ON TABLE public.tb_host_orchestrator IS 'Host de execução de instalações do produto por cliente. Distinto do campo host de ConfigEntrega (destino FTP/SFTP).';

COMMENT ON COLUMN public.tb_host_orchestrator.codigo IS 'Identificador curto único, normalizado em maiúsculas (ex.: SRV-WIN-01).';

COMMENT ON COLUMN public.tb_host_orchestrator.hostname IS 'Nome DNS ou NetBIOS da máquina.';

COMMENT ON COLUMN public.tb_host_orchestrator.endereco_ip IS 'IPv4 ou IPv6 opcional. Não substitui o hostname.';

COMMENT ON COLUMN public.tb_host_orchestrator.sistema_operacional IS 'WINDOWS ou LINUX. O tipo de implantação (installer/systemd/compose) fica na instalação, não no host.';

COMMENT ON COLUMN public.tb_host_orchestrator.docker_disponivel IS 'TRUE quando o host está destinado à execução Docker Compose.';

COMMENT ON COLUMN public.tb_host_orchestrator.tipo_conexao IS 'SSH, WINRM ou DOCKER (API). Usado nas fases posteriores de deploy.';

COMMENT ON COLUMN public.tb_host_orchestrator.credencial_ref IS 'Referência a segredo (cofre/vault). Nunca armazena senha em texto puro.';

-- RBAC: funcionalidade HOST no domínio RELEASE_ORCHESTRATOR
INSERT INTO public.tb_funcionalidade (id, dominio_id, codigo, nome, descricao, ativo, created_at, updated_at, created_by, updated_by) VALUES
	('00000000-0000-0000-0200-000000000026', '00000000-0000-0000-0100-000000000004', 'HOST', 'Host', 'Hosts de execução das instalações do produto por cliente.', true, now(), now(), 'seed', 'seed');

INSERT INTO public.tb_permissao (id, funcionalidade_id, acao, codigo, descricao, ativo, created_at, updated_at, created_by, updated_by) VALUES
	('0449f5ec-06a3-4a18-9fcd-afc31c536a85', '00000000-0000-0000-0200-000000000026', 'LER', 'HOST:LER', NULL, true, now(), now(), 'seed', 'seed'),
	('8e796b26-92b3-4ca6-bf6a-e943388be7a5', '00000000-0000-0000-0200-000000000026', 'CRIAR', 'HOST:CRIAR', NULL, true, now(), now(), 'seed', 'seed'),
	('525f545f-7ec7-452a-b7e9-082b59a346ac', '00000000-0000-0000-0200-000000000026', 'EDITAR', 'HOST:EDITAR', NULL, true, now(), now(), 'seed', 'seed'),
	('5291f372-746a-4e45-b7f7-66f2f53dd831', '00000000-0000-0000-0200-000000000026', 'EXCLUIR', 'HOST:EXCLUIR', NULL, true, now(), now(), 'seed', 'seed');

-- ADMIN: CRUD | EDITOR: CRUD | LEITOR/REVISOR: LER
INSERT INTO public.tb_grupo_permissao (grupo_id, permissao_id) VALUES
	('00000000-0000-0000-0000-000000000801', '0449f5ec-06a3-4a18-9fcd-afc31c536a85'),
	('00000000-0000-0000-0000-000000000801', '8e796b26-92b3-4ca6-bf6a-e943388be7a5'),
	('00000000-0000-0000-0000-000000000801', '525f545f-7ec7-452a-b7e9-082b59a346ac'),
	('00000000-0000-0000-0000-000000000801', '5291f372-746a-4e45-b7f7-66f2f53dd831'),
	('00000000-0000-0000-0000-000000000802', '0449f5ec-06a3-4a18-9fcd-afc31c536a85'),
	('00000000-0000-0000-0000-000000000802', '8e796b26-92b3-4ca6-bf6a-e943388be7a5'),
	('00000000-0000-0000-0000-000000000802', '525f545f-7ec7-452a-b7e9-082b59a346ac'),
	('00000000-0000-0000-0000-000000000802', '5291f372-746a-4e45-b7f7-66f2f53dd831'),
	('00000000-0000-0000-0000-000000000803', '0449f5ec-06a3-4a18-9fcd-afc31c536a85'),
	('00000000-0000-0000-0000-000000000804', '0449f5ec-06a3-4a18-9fcd-afc31c536a85');
