-- =============================================================================
-- MÓDULO: docflow | ETAPA 1–2: schema lógico + tabelas
-- Usa schema PostgreSQL `public` (padrão do projeto). Prefixo de tabelas: tb_
-- Manuais: clientes, projetos, módulos, páginas, publicações, vínculos, preview
-- =============================================================================

CREATE TABLE tb_cliente (
  id                UUID         NOT NULL,
  nome              VARCHAR(150) NOT NULL,
  slug              VARCHAR(150) NOT NULL,
  ativo             BOOLEAN      NOT NULL DEFAULT TRUE,
  logo_path         VARCHAR(500),
  logo_content_type VARCHAR(100),
  tema_cor_primaria VARCHAR(20),
  tema_cor_fundo    VARCHAR(20),
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by        VARCHAR(120),
  updated_by        VARCHAR(120),
  CONSTRAINT pk_tb_cliente PRIMARY KEY (id)
);

CREATE TABLE tb_projeto (
  id         UUID         NOT NULL,
  nome       VARCHAR(150) NOT NULL,
  slug       VARCHAR(150) NOT NULL,
  descricao  TEXT,
  ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  CONSTRAINT pk_tb_projeto PRIMARY KEY (id)
);

CREATE TABLE tb_modulo (
  id         UUID         NOT NULL,
  projeto_id UUID         NOT NULL,
  nome       VARCHAR(150) NOT NULL,
  slug       VARCHAR(150) NOT NULL,
  descricao  TEXT,
  ordem      INT          NOT NULL DEFAULT 0,
  ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  CONSTRAINT pk_tb_modulo PRIMARY KEY (id)
);

CREATE TABLE tb_pagina (
  id            UUID         NOT NULL,
  modulo_id     UUID         NOT NULL,
  parent_id     UUID,
  titulo        VARCHAR(200) NOT NULL,
  slug          VARCHAR(200) NOT NULL,
  codigo_tela   VARCHAR(120) NOT NULL,
  resumo        TEXT,
  conteudo_html TEXT,
  status        VARCHAR(30)  NOT NULL,
  ordem         INT          NOT NULL DEFAULT 0,
  ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
  published_at  TIMESTAMPTZ,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by    VARCHAR(120),
  updated_by    VARCHAR(120),
  search_vector TSVECTOR,
  CONSTRAINT pk_tb_pagina PRIMARY KEY (id)
);

CREATE TABLE tb_pagina_revisao (
  id            UUID         NOT NULL,
  pagina_id     UUID         NOT NULL,
  numero        INT          NOT NULL,
  titulo        VARCHAR(200) NOT NULL,
  slug          VARCHAR(200) NOT NULL,
  codigo_tela   VARCHAR(120) NOT NULL,
  resumo        TEXT,
  conteudo_html TEXT,
  status        VARCHAR(30)  NOT NULL,
  modulo_id     UUID         NOT NULL,
  parent_id     UUID,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by    VARCHAR(120),
  CONSTRAINT pk_tb_pagina_revisao PRIMARY KEY (id)
);

CREATE TABLE tb_pagina_anexo (
  id            UUID         NOT NULL,
  pagina_id     UUID         NOT NULL,
  nome_original VARCHAR(255) NOT NULL,
  content_type  VARCHAR(120) NOT NULL,
  tamanho_bytes BIGINT       NOT NULL,
  caminho       VARCHAR(700) NOT NULL,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by    VARCHAR(120),
  updated_by    VARCHAR(120),
  CONSTRAINT pk_tb_pagina_anexo PRIMARY KEY (id)
);

CREATE TABLE tb_publicacao (
  id                  UUID         NOT NULL,
  cliente_id          UUID         NOT NULL,
  versao              VARCHAR(50)  NOT NULL,
  status              VARCHAR(30)  NOT NULL,
  quantidade_paginas  INT          NOT NULL DEFAULT 0,
  quantidade_modulos  INT          NOT NULL DEFAULT 0,
  arquivo_zip_nome    VARCHAR(255),
  arquivo_zip_caminho VARCHAR(500),
  hash_pacote         VARCHAR(120),
  observacao          TEXT,
  relatorio_validacao VARCHAR(4000),
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by          VARCHAR(120),
  updated_by          VARCHAR(120),
  CONSTRAINT pk_tb_publicacao PRIMARY KEY (id)
);

CREATE TABLE tb_cliente_projeto (
  id         UUID NOT NULL,
  cliente_id UUID NOT NULL,
  projeto_id UUID NOT NULL,
  CONSTRAINT pk_tb_cliente_projeto PRIMARY KEY (id)
);

CREATE TABLE tb_cliente_modulo (
  id         UUID NOT NULL,
  cliente_id UUID NOT NULL,
  modulo_id  UUID NOT NULL,
  CONSTRAINT pk_tb_cliente_modulo PRIMARY KEY (id)
);

CREATE TABLE tb_cliente_pagina (
  id         UUID NOT NULL,
  cliente_id UUID NOT NULL,
  pagina_id  UUID NOT NULL,
  CONSTRAINT pk_tb_cliente_pagina PRIMARY KEY (id)
);

CREATE TABLE tb_auditoria_evento (
  id          UUID        NOT NULL,
  entidade    VARCHAR(80) NOT NULL,
  entidade_id UUID,
  acao        VARCHAR(80) NOT NULL,
  descricao   TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by  VARCHAR(120),
  CONSTRAINT pk_tb_auditoria_evento PRIMARY KEY (id)
);

CREATE TABLE tb_preview_token (
  id         UUID         NOT NULL,
  cliente_id UUID         NOT NULL,
  token      VARCHAR(120) NOT NULL,
  expires_at TIMESTAMPTZ  NOT NULL,
  ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by VARCHAR(120),
  CONSTRAINT pk_tb_preview_token PRIMARY KEY (id)
);

CREATE TABLE tb_publicacao_changelog (
  id            UUID         NOT NULL,
  publicacao_id UUID         NOT NULL,
  pagina_id     UUID,
  pagina_titulo VARCHAR(200) NOT NULL,
  tipo_mudanca  VARCHAR(30)  NOT NULL,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT pk_tb_publicacao_changelog PRIMARY KEY (id)
);
