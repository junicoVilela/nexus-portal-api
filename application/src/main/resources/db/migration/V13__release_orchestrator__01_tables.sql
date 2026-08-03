-- Release Orchestrator — tables

CREATE TABLE public.tb_produto_rh (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome character varying(200) NOT NULL,
    sigla character varying(20) NOT NULL,
    descricao character varying(500),
    cor character varying(20) DEFAULT '#2563eb'::character varying NOT NULL,
    responsavel_id uuid,
    ativo boolean DEFAULT true NOT NULL,
    repositorio_github character varying(200),
    branch_padrao character varying(80) DEFAULT 'main'::character varying,
    padrao_tag character varying(200) DEFAULT '^v\d+\.\d+\.\d+$'::character varying,
    github_token character varying(500),
    jenkins_url character varying(300),
    jenkins_job character varying(200),
    jenkins_user character varying(120),
    jenkins_token character varying(500),
    jenkins_trigger_mode character varying(30) DEFAULT 'BUILD_ON_TAG'::character varying,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_produto_rh_jenkins_trigger CHECK (((jenkins_trigger_mode IS NULL) OR ((jenkins_trigger_mode)::text = ANY ((ARRAY['BUILD_ON_TAG'::character varying, 'MANUAL'::character varying])::text[]))))
);

CREATE TABLE public.tb_release (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    produto_id uuid NOT NULL,
    versao character varying(50) NOT NULL,
    titulo character varying(200) NOT NULL,
    tipo character varying(30) NOT NULL,
    status character varying(30) DEFAULT 'RASCUNHO'::character varying NOT NULL,
    data_prevista date,
    data_publicacao date,
    publicado_por character varying(120),
    responsavel_id uuid,
    resumo text,
    observacoes text,
    ultimo_build_status character varying(30),
    ultimo_build_numero integer,
    ultimo_build_url character varying(500),
    ultimo_build_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_release_build_status CHECK (((ultimo_build_status IS NULL) OR ((ultimo_build_status)::text = ANY ((ARRAY['EM_ANDAMENTO'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'UNSTABLE'::character varying, 'ABORTED'::character varying])::text[]))))
);

CREATE TABLE public.tb_release_item (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    release_id uuid NOT NULL,
    categoria character varying(40) NOT NULL,
    titulo character varying(300) NOT NULL,
    descricao text,
    visibilidade character varying(20) DEFAULT 'TODOS'::character varying NOT NULL,
    ordem integer DEFAULT 0 NOT NULL,
    ticket character varying(100),
    commit_hash character varying(100),
    pull_request character varying(100),
    responsavel_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_release_historico (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    release_id uuid NOT NULL,
    acao character varying(40) NOT NULL,
    descricao character varying(500),
    status_anterior character varying(30),
    status_novo character varying(30),
    usuario character varying(120),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.tb_release_template (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome character varying(200) NOT NULL,
    descricao character varying(500),
    tipo_release character varying(30),
    produto_id uuid,
    estrutura text,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_modulo_produto (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    produto_id uuid NOT NULL,
    codigo character varying(80) NOT NULL,
    nome character varying(200) NOT NULL,
    tipo character varying(30) NOT NULL,
    gera_delta boolean NOT NULL,
    obrigatorio boolean NOT NULL,
    ordem integer DEFAULT 0 NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    config_especifica text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_modulo_produto_tipo CHECK (((tipo)::text = ANY ((ARRAY['WEB'::character varying, 'BATCH'::character varying, 'BANCO'::character varying, 'KETTLE'::character varying, 'FUNCIONALIDADES'::character varying, 'REGRAS'::character varying])::text[])))
);

CREATE TABLE public.tb_release_modulo_versao (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    release_id uuid NOT NULL,
    modulo_produto_id uuid NOT NULL,
    versao character varying(80) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_artefato_release_modulo (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    release_id uuid NOT NULL,
    modulo_produto_id uuid NOT NULL,
    nome_arquivo character varying(255) NOT NULL,
    caminho_armazenado character varying(700) NOT NULL,
    sha256 character varying(64) NOT NULL,
    tamanho_bytes bigint NOT NULL,
    observacao text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_cliente_orchestrator (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome character varying(200) NOT NULL,
    razao_social character varying(300),
    cnpj character varying(18),
    sigla character varying(20) NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    responsavel_comercial_id uuid,
    ambiente_padrao character varying(20) NOT NULL,
    tipo_banco character varying(20),
    codificacao character varying(30),
    fuso_horario character varying(60),
    observacoes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_cliente_orchestrator_ambiente CHECK (((ambiente_padrao)::text = ANY ((ARRAY['PROD'::character varying, 'HOM'::character varying, 'DEV'::character varying, 'TEST'::character varying])::text[]))),
    CONSTRAINT ck_tb_cliente_orchestrator_banco CHECK (((tipo_banco IS NULL) OR ((tipo_banco)::text = ANY ((ARRAY['ORACLE'::character varying, 'SQLSERVER'::character varying, 'POSTGRES'::character varying])::text[]))))
);

CREATE TABLE public.tb_contato_orchestrator (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cliente_id uuid NOT NULL,
    nome character varying(200) NOT NULL,
    papel character varying(20) NOT NULL,
    email character varying(200) NOT NULL,
    telefone character varying(40),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_contato_orchestrator_papel CHECK (((papel)::text = ANY ((ARRAY['TECNICO'::character varying, 'COMERCIAL'::character varying, 'OPERACIONAL'::character varying, 'FINANCEIRO'::character varying, 'OUTRO'::character varying])::text[])))
);

CREATE TABLE public.tb_config_entrega_orchestrator (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cliente_id uuid NOT NULL,
    tipo_destino character varying(20) NOT NULL,
    caminho_base character varying(500),
    exigir_aprovacao boolean DEFAULT false NOT NULL,
    emails_notificacao character varying(1000),
    host character varying(200),
    porta integer,
    usuario character varying(120),
    senha_cifrada character varying(1000),
    modo_passivo boolean DEFAULT true,
    strict_host_check boolean DEFAULT true,
    bucket character varying(200),
    endpoint character varying(500),
    regiao character varying(60),
    path_style_access boolean DEFAULT false,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_config_entrega_orchestrator_tipo CHECK (((tipo_destino)::text = ANY ((ARRAY['PASTA'::character varying, 'FTP'::character varying, 'SFTP'::character varying, 'BUCKET'::character varying])::text[])))
);

CREATE TABLE public.tb_dominio_produto (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    produto_id uuid NOT NULL,
    nome character varying(150) NOT NULL,
    codigo character varying(80) NOT NULL,
    codigo_legado character varying(80),
    descricao character varying(1000),
    ordem integer DEFAULT 0 NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_funcionalidade_produto (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    dominio_produto_id uuid NOT NULL,
    nome character varying(200) NOT NULL,
    codigo character varying(80) NOT NULL,
    codigo_legado character varying(80),
    codigo_operacao character varying(80),
    descricao character varying(1000),
    critica boolean DEFAULT false NOT NULL,
    ordem integer DEFAULT 0 NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_cliente_funcionalidade_produto (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cliente_id uuid NOT NULL,
    funcionalidade_produto_id uuid CONSTRAINT tb_cliente_funcionalidade_pr_funcionalidade_produto_id_not_null NOT NULL,
    habilitada boolean NOT NULL,
    origem character varying(20) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_cliente_funcionalidade_produto_origem CHECK (((origem)::text = ANY ((ARRAY['MANUAL'::character varying, 'TEMPLATE'::character varying, 'HERDADA'::character varying])::text[])))
);

CREATE TABLE public.tb_cliente_produto (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cliente_id uuid NOT NULL,
    produto_id uuid NOT NULL,
    ambiente character varying(20) NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_cliente_produto_ambiente CHECK (((ambiente)::text = ANY ((ARRAY['PROD'::character varying, 'HOM'::character varying, 'DEV'::character varying, 'TEST'::character varying])::text[])))
);

CREATE TABLE public.tb_cliente_produto_modulo (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cliente_produto_id uuid NOT NULL,
    modulo_produto_id uuid NOT NULL,
    versao_atual character varying(80),
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_proxima_entrega (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cliente_id uuid NOT NULL,
    produto_id uuid NOT NULL,
    release_id uuid,
    data_prevista date NOT NULL,
    ambiente character varying(20) NOT NULL,
    prioridade character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    responsavel_id uuid,
    observacoes text,
    entrega_convertida_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_proxima_entrega_ambiente CHECK (((ambiente)::text = ANY ((ARRAY['PROD'::character varying, 'HOM'::character varying, 'DEV'::character varying, 'TEST'::character varying])::text[]))),
    CONSTRAINT ck_tb_proxima_entrega_prioridade CHECK (((prioridade)::text = ANY ((ARRAY['BAIXA'::character varying, 'MEDIA'::character varying, 'ALTA'::character varying, 'CRITICA'::character varying])::text[]))),
    CONSTRAINT ck_tb_proxima_entrega_status CHECK (((status)::text = ANY ((ARRAY['PLANEJADA'::character varying, 'AGENDADA'::character varying, 'REPLANEJADA'::character varying, 'ATRASADA'::character varying, 'CONVERTIDA'::character varying, 'CANCELADA'::character varying])::text[])))
);

CREATE TABLE public.tb_entrega (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cliente_id uuid NOT NULL,
    produto_id uuid NOT NULL,
    release_id uuid NOT NULL,
    proxima_entrega_id uuid,
    entrega_original_id uuid,
    ambiente character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    data_inicio_geracao timestamp with time zone,
    data_conclusao timestamp with time zone,
    responsavel_id uuid,
    arquivo_pacote_caminho character varying(700),
    arquivo_pacote_sha256 character varying(64),
    tamanho_bytes bigint,
    observacoes text,
    falha_motivo text,
    status_publicacao character varying(20) DEFAULT 'NAO_APLICAVEL'::character varying NOT NULL,
    tentativas_publicacao integer DEFAULT 0 NOT NULL,
    proxima_tentativa_em timestamp with time zone,
    ultima_falha_publicacao text,
    data_publicacao timestamp with time zone,
    destino_publicacao character varying(700),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_entrega_ambiente CHECK (((ambiente)::text = ANY ((ARRAY['PROD'::character varying, 'HOM'::character varying, 'DEV'::character varying, 'TEST'::character varying])::text[]))),
    CONSTRAINT ck_tb_entrega_status CHECK (((status)::text = ANY ((ARRAY['RASCUNHO'::character varying, 'EM_GERACAO'::character varying, 'CONCLUIDA'::character varying, 'FALHA'::character varying, 'CANCELADA'::character varying])::text[])))
);

CREATE TABLE public.tb_entrega_modulo (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    entrega_id uuid NOT NULL,
    modulo_produto_id uuid NOT NULL,
    versao_from character varying(80),
    versao_to character varying(80),
    selecionado boolean NOT NULL,
    fora_contrato boolean DEFAULT false NOT NULL,
    ordem integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_entrega_modulo_artefato (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    entrega_modulo_id uuid NOT NULL,
    artefato_release_modulo_id uuid NOT NULL,
    ordem integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);
