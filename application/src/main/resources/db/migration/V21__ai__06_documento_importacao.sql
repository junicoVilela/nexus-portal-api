-- Nexus AI — importação estruturada de manuais (arquivo -> projeto -> módulos -> páginas)

CREATE TABLE public.tb_ai_documento_importacao (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome_arquivo character varying(255) NOT NULL,
    tipo_arquivo character varying(10) NOT NULL,
    mime_type character varying(160) NOT NULL,
    tamanho_bytes bigint NOT NULL,
    hash_sha256 character varying(64) NOT NULL,
    texto_extraido text NOT NULL,
    total_paginas_origem integer NOT NULL,
    status character varying(40) NOT NULL,
    plano_json jsonb NOT NULL,
    avisos_json jsonb NOT NULL DEFAULT '[]'::jsonb,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120),
    CONSTRAINT pk_tb_ai_documento_importacao PRIMARY KEY (id),
    CONSTRAINT ck_tb_ai_documento_tipo CHECK (tipo_arquivo IN ('DOCX', 'PDF', 'TXT')),
    CONSTRAINT ck_tb_ai_documento_status CHECK (
        status IN ('PRONTO_PARA_REVISAO', 'EM_REVISAO', 'CONCLUIDA')
    ),
    CONSTRAINT ck_tb_ai_documento_tamanho CHECK (tamanho_bytes > 0 AND tamanho_bytes <= 15728640),
    CONSTRAINT ck_tb_ai_documento_paginas CHECK (total_paginas_origem >= 1)
);

CREATE INDEX ix_tb_ai_documento_importacao_usuario
    ON public.tb_ai_documento_importacao (created_by, created_at DESC);
CREATE INDEX ix_tb_ai_documento_importacao_hash
    ON public.tb_ai_documento_importacao (hash_sha256);

COMMENT ON COLUMN public.tb_ai_documento_importacao.texto_extraido IS
    'Texto extraído para revisão e reprocessamento; o arquivo binário original não é persistido.';
COMMENT ON COLUMN public.tb_ai_documento_importacao.plano_json IS
    'Plano ordenado e versionado de projeto, módulos e páginas sugeridas para geração.';
