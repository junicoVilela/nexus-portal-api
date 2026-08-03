-- DocFlow — tables

CREATE TABLE public.tb_cliente (
    id uuid NOT NULL,
    nome character varying(150) NOT NULL,
    slug character varying(150) NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    logo_path character varying(500),
    logo_content_type character varying(100),
    tema_cor_primaria character varying(20),
    tema_cor_fundo character varying(20),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_projeto (
    id uuid NOT NULL,
    nome character varying(150) NOT NULL,
    slug character varying(150) NOT NULL,
    descricao text,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_modulo (
    id uuid NOT NULL,
    projeto_id uuid NOT NULL,
    nome character varying(150) NOT NULL,
    slug character varying(150) NOT NULL,
    descricao text,
    ordem integer DEFAULT 0 NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_pagina (
    id uuid NOT NULL,
    modulo_id uuid NOT NULL,
    parent_id uuid,
    titulo character varying(200) NOT NULL,
    slug character varying(200) NOT NULL,
    codigo_tela character varying(120) NOT NULL,
    resumo text,
    conteudo_html text,
    status character varying(30) NOT NULL,
    ordem integer DEFAULT 0 NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    published_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    search_vector tsvector,
    version bigint DEFAULT 0 NOT NULL,
    template_origem_id uuid,
    template_origem_versao integer
);

CREATE TABLE public.tb_pagina_revisao (
    id uuid NOT NULL,
    pagina_id uuid NOT NULL,
    numero integer NOT NULL,
    titulo character varying(200) NOT NULL,
    slug character varying(200) NOT NULL,
    codigo_tela character varying(120) NOT NULL,
    resumo text,
    conteudo_html text,
    status character varying(30) NOT NULL,
    modulo_id uuid,
    parent_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    tipo character varying(40) DEFAULT 'SALVAMENTO_MANUAL'::character varying NOT NULL,
    descricao character varying(300)
);

CREATE TABLE public.tb_pagina_anexo (
    id uuid NOT NULL,
    pagina_id uuid NOT NULL,
    nome_original character varying(255) NOT NULL,
    content_type character varying(120) NOT NULL,
    tamanho_bytes bigint NOT NULL,
    caminho character varying(700) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_publicacao (
    id uuid NOT NULL,
    cliente_id uuid NOT NULL,
    versao character varying(50) NOT NULL,
    status character varying(30) NOT NULL,
    quantidade_paginas integer DEFAULT 0 NOT NULL,
    quantidade_modulos integer DEFAULT 0 NOT NULL,
    arquivo_zip_nome character varying(255),
    arquivo_zip_caminho character varying(500),
    hash_pacote character varying(120),
    observacao text,
    relatorio_validacao character varying(4000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    arvore_paginas text
);

CREATE TABLE public.tb_cliente_projeto (
    id uuid NOT NULL,
    cliente_id uuid NOT NULL,
    projeto_id uuid NOT NULL
);

CREATE TABLE public.tb_cliente_modulo (
    id uuid NOT NULL,
    cliente_id uuid NOT NULL,
    modulo_id uuid NOT NULL
);

CREATE TABLE public.tb_cliente_pagina (
    id uuid NOT NULL,
    cliente_id uuid NOT NULL,
    pagina_id uuid NOT NULL
);

CREATE TABLE public.tb_auditoria_evento (
    id uuid NOT NULL,
    entidade character varying(80) NOT NULL,
    entidade_id uuid,
    acao character varying(80) NOT NULL,
    descricao text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120)
);

CREATE TABLE public.tb_preview_token (
    id uuid NOT NULL,
    cliente_id uuid NOT NULL,
    token character varying(120) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120)
);

CREATE TABLE public.tb_publicacao_changelog (
    id uuid NOT NULL,
    publicacao_id uuid NOT NULL,
    pagina_id uuid,
    pagina_titulo character varying(200) NOT NULL,
    tipo_mudanca character varying(30) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.tb_pagina_template (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    codigo character varying(60) NOT NULL,
    nome character varying(120) NOT NULL,
    descricao character varying(300),
    conteudo_html text NOT NULL,
    ordem integer DEFAULT 0 NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    personalizado boolean DEFAULT false NOT NULL,
    projeto_id uuid,
    cliente_id uuid,
    versao_atual integer DEFAULT 1 NOT NULL,
    CONSTRAINT ck_pagina_template_escopo CHECK ((((personalizado = false) AND (projeto_id IS NULL) AND (cliente_id IS NULL)) OR ((personalizado = true) AND ((projeto_id IS NULL) <> (cliente_id IS NULL)))))
);

CREATE TABLE public.tb_pagina_template_versao (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    template_id uuid NOT NULL,
    numero integer NOT NULL,
    nome character varying(120) NOT NULL,
    descricao character varying(300),
    conteudo_html text NOT NULL,
    ativo boolean NOT NULL,
    projeto_id uuid,
    cliente_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(150) DEFAULT 'system'::character varying NOT NULL,
    updated_by character varying(150) DEFAULT 'system'::character varying NOT NULL,
    CONSTRAINT ck_pagina_template_versao_escopo CHECK ((((projeto_id IS NULL) <> (cliente_id IS NULL)) OR ((projeto_id IS NULL) AND (cliente_id IS NULL))))
);

CREATE TABLE public.tb_ajuda_conteudo (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    codigo character varying(80) NOT NULL,
    tipo character varying(30) NOT NULL,
    jornada_codigo character varying(80),
    titulo character varying(160) NOT NULL,
    resumo character varying(400),
    conteudo text,
    rota_contexto character varying(220),
    rota_acao character varying(220),
    rotulo_acao character varying(80),
    icone character varying(50),
    seletor_alvo character varying(200),
    media_tipo character varying(20) DEFAULT 'NENHUMA'::character varying NOT NULL,
    media_urls text,
    media_alt character varying(240),
    ordem integer DEFAULT 0 NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_ajuda_conteudo_jornada_tipo CHECK (((((tipo)::text = 'ETAPA'::text) AND (jornada_codigo IS NOT NULL)) OR (((tipo)::text <> 'ETAPA'::text) AND (jornada_codigo IS NULL))))
);

CREATE TABLE public.tb_ajuda_evento (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tipo character varying(30) NOT NULL,
    conteudo_codigo character varying(80),
    termo character varying(240),
    rota character varying(240),
    sessao_id character varying(80),
    resultado_quantidade integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120)
);
