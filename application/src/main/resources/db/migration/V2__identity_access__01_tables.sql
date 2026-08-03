-- Identity Access — tables

CREATE TABLE public.tb_usuario (
    id uuid NOT NULL,
    username character varying(80) NOT NULL,
    password character varying(255) NOT NULL,
    nome character varying(150),
    email character varying(200),
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    bloqueado boolean DEFAULT false NOT NULL,
    tentativas_invalidas integer DEFAULT 0 NOT NULL,
    trocar_senha_proximo_login boolean DEFAULT false NOT NULL
);

CREATE TABLE public.tb_dominio (
    id uuid NOT NULL,
    codigo character varying(80) NOT NULL,
    nome character varying(150) NOT NULL,
    descricao character varying(500),
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_funcionalidade (
    id uuid NOT NULL,
    dominio_id uuid NOT NULL,
    codigo character varying(80) NOT NULL,
    nome character varying(150) NOT NULL,
    descricao character varying(500),
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_permissao (
    id uuid NOT NULL,
    funcionalidade_id uuid NOT NULL,
    acao character varying(40) NOT NULL,
    codigo character varying(120) NOT NULL,
    descricao character varying(500),
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_grupo (
    id uuid NOT NULL,
    codigo character varying(80) NOT NULL,
    nome character varying(150) NOT NULL,
    descricao character varying(500),
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_grupo_permissao (
    grupo_id uuid NOT NULL,
    permissao_id uuid NOT NULL
);

CREATE TABLE public.tb_grupo_usuario (
    grupo_id uuid NOT NULL,
    usuario_id uuid NOT NULL
);

CREATE TABLE public.tb_historico_login (
    id uuid NOT NULL,
    usuario_id uuid,
    login_informado character varying(120) NOT NULL,
    ip_origem character varying(45),
    user_agent character varying(500),
    sucesso boolean NOT NULL,
    motivo_falha character varying(200),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.tb_politica_senha (
    id uuid NOT NULL,
    tamanho_minimo integer DEFAULT 8 NOT NULL,
    exigir_maiuscula boolean DEFAULT true NOT NULL,
    exigir_minuscula boolean DEFAULT true NOT NULL,
    exigir_numero boolean DEFAULT true NOT NULL,
    exigir_especial boolean DEFAULT false NOT NULL,
    expira_senha_dias integer,
    quantidade_historico integer DEFAULT 3 NOT NULL,
    max_tentativas_invalidas integer DEFAULT 5 NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

CREATE TABLE public.tb_historico_senha (
    id uuid NOT NULL,
    usuario_id uuid NOT NULL,
    senha_hash character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.tb_sessao (
    id uuid NOT NULL,
    jti character varying(120) NOT NULL,
    usuario_id uuid NOT NULL,
    ip_origem character varying(45),
    user_agent character varying(500),
    ativa boolean DEFAULT true NOT NULL,
    revogada boolean DEFAULT false NOT NULL,
    motivo_encerramento character varying(200),
    iniciada_em timestamp with time zone DEFAULT now() NOT NULL,
    encerrada_em timestamp with time zone,
    expira_em timestamp with time zone
);

CREATE TABLE public.tb_acesso_temporario (
    id uuid NOT NULL,
    usuario_id uuid NOT NULL,
    grupo_id uuid,
    permissao_id uuid,
    escopo_id uuid,
    inicio_em timestamp with time zone NOT NULL,
    fim_em timestamp with time zone NOT NULL,
    justificativa character varying(500),
    revogado_em timestamp with time zone,
    motivo_revogacao character varying(200),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    CONSTRAINT ck_tb_acesso_temporario_alvo CHECK (((grupo_id IS NOT NULL) OR (permissao_id IS NOT NULL) OR (escopo_id IS NOT NULL))),
    CONSTRAINT ck_tb_acesso_temporario_janela CHECK ((fim_em > inicio_em))
);

CREATE TABLE public.tb_escopo_acesso (
    id uuid NOT NULL,
    usuario_id uuid,
    grupo_id uuid,
    cliente_id uuid,
    ambiente_id uuid,
    produto_id uuid,
    tipo_ambiente character varying(20),
    somente_leitura boolean DEFAULT false NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT ck_tb_escopo_acesso_alvo CHECK (((usuario_id IS NOT NULL) OR (grupo_id IS NOT NULL)))
);
