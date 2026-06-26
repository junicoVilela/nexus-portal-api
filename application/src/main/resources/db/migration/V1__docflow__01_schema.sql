-- =============================================================================
-- MÓDULO: docflow | BASELINE: schema (tabelas + constraints + índices)
-- Schema PostgreSQL `public`. Prefixo de tabelas: tb_
-- Manuais: clientes, projetos, módulos, páginas, publicações, vínculos, preview
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabelas (constraints declaradas inline; nada de ALTER TABLE posterior).
-- -----------------------------------------------------------------------------

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
  CONSTRAINT pk_tb_cliente PRIMARY KEY (id),
  CONSTRAINT uq_tb_cliente_slug UNIQUE (slug)
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
  CONSTRAINT pk_tb_projeto PRIMARY KEY (id),
  CONSTRAINT uq_tb_projeto_slug UNIQUE (slug)
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
  CONSTRAINT pk_tb_modulo PRIMARY KEY (id),
  CONSTRAINT uq_tb_modulo_projeto_slug UNIQUE (projeto_id, slug),
  CONSTRAINT fk_tb_modulo_projeto FOREIGN KEY (projeto_id) REFERENCES tb_projeto(id) ON DELETE CASCADE
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
  CONSTRAINT pk_tb_pagina PRIMARY KEY (id),
  CONSTRAINT uq_tb_pagina_slug UNIQUE (slug),
  CONSTRAINT uq_tb_pagina_codigo_tela UNIQUE (codigo_tela),
  CONSTRAINT fk_tb_pagina_modulo FOREIGN KEY (modulo_id) REFERENCES tb_modulo(id),
  CONSTRAINT fk_tb_pagina_parent FOREIGN KEY (parent_id) REFERENCES tb_pagina(id) ON DELETE CASCADE
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
  CONSTRAINT pk_tb_pagina_revisao PRIMARY KEY (id),
  CONSTRAINT fk_tb_pagina_revisao_pagina FOREIGN KEY (pagina_id) REFERENCES tb_pagina(id) ON DELETE CASCADE,
  CONSTRAINT fk_tb_pagina_revisao_modulo FOREIGN KEY (modulo_id) REFERENCES tb_modulo(id),
  CONSTRAINT fk_tb_pagina_revisao_parent FOREIGN KEY (parent_id) REFERENCES tb_pagina(id)
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
  CONSTRAINT pk_tb_pagina_anexo PRIMARY KEY (id),
  CONSTRAINT fk_tb_pagina_anexo_pagina FOREIGN KEY (pagina_id) REFERENCES tb_pagina(id) ON DELETE CASCADE
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
  CONSTRAINT pk_tb_publicacao PRIMARY KEY (id),
  CONSTRAINT fk_tb_publicacao_cliente FOREIGN KEY (cliente_id) REFERENCES tb_cliente(id)
);

CREATE TABLE tb_cliente_projeto (
  id         UUID NOT NULL,
  cliente_id UUID NOT NULL,
  projeto_id UUID NOT NULL,
  CONSTRAINT pk_tb_cliente_projeto PRIMARY KEY (id),
  CONSTRAINT uq_tb_cliente_projeto UNIQUE (cliente_id, projeto_id),
  CONSTRAINT fk_tb_cliente_projeto_cliente FOREIGN KEY (cliente_id) REFERENCES tb_cliente(id) ON DELETE CASCADE,
  CONSTRAINT fk_tb_cliente_projeto_projeto FOREIGN KEY (projeto_id) REFERENCES tb_projeto(id) ON DELETE CASCADE
);

CREATE TABLE tb_cliente_modulo (
  id         UUID NOT NULL,
  cliente_id UUID NOT NULL,
  modulo_id  UUID NOT NULL,
  CONSTRAINT pk_tb_cliente_modulo PRIMARY KEY (id),
  CONSTRAINT uq_tb_cliente_modulo UNIQUE (cliente_id, modulo_id),
  CONSTRAINT fk_tb_cliente_modulo_cliente FOREIGN KEY (cliente_id) REFERENCES tb_cliente(id) ON DELETE CASCADE,
  CONSTRAINT fk_tb_cliente_modulo_modulo  FOREIGN KEY (modulo_id)  REFERENCES tb_modulo(id)  ON DELETE CASCADE
);

CREATE TABLE tb_cliente_pagina (
  id         UUID NOT NULL,
  cliente_id UUID NOT NULL,
  pagina_id  UUID NOT NULL,
  CONSTRAINT pk_tb_cliente_pagina PRIMARY KEY (id),
  CONSTRAINT uq_tb_cliente_pagina UNIQUE (cliente_id, pagina_id),
  CONSTRAINT fk_tb_cliente_pagina_cliente FOREIGN KEY (cliente_id) REFERENCES tb_cliente(id) ON DELETE CASCADE,
  CONSTRAINT fk_tb_cliente_pagina_pagina  FOREIGN KEY (pagina_id)  REFERENCES tb_pagina(id)  ON DELETE CASCADE
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
  CONSTRAINT pk_tb_preview_token PRIMARY KEY (id),
  CONSTRAINT uq_tb_preview_token_token UNIQUE (token),
  CONSTRAINT fk_tb_preview_token_cliente FOREIGN KEY (cliente_id) REFERENCES tb_cliente(id)
);

CREATE TABLE tb_publicacao_changelog (
  id            UUID         NOT NULL,
  publicacao_id UUID         NOT NULL,
  pagina_id     UUID,
  pagina_titulo VARCHAR(200) NOT NULL,
  tipo_mudanca  VARCHAR(30)  NOT NULL,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT pk_tb_publicacao_changelog PRIMARY KEY (id),
  CONSTRAINT fk_tb_publicacao_changelog_publicacao FOREIGN KEY (publicacao_id) REFERENCES tb_publicacao(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- Comentários de coluna
-- -----------------------------------------------------------------------------

COMMENT ON COLUMN tb_cliente.tema_cor_primaria       IS 'Cor accent do manual (manifest/PWA); ex.: #1a73e8';
COMMENT ON COLUMN tb_cliente.tema_cor_fundo          IS 'Cor de fundo base do tema; ex.: #f8f9fa';
COMMENT ON COLUMN tb_publicacao.relatorio_validacao  IS 'JSON com resultado da validação do ZIP (arquivos obrigatórios, etc.)';

-- -----------------------------------------------------------------------------
-- Índices
-- -----------------------------------------------------------------------------

CREATE INDEX idx_tb_modulo_projeto             ON tb_modulo              (projeto_id);
CREATE INDEX idx_tb_pagina_modulo_status       ON tb_pagina              (modulo_id, status);
CREATE INDEX idx_tb_pagina_parent_ordem        ON tb_pagina              (parent_id, ordem);
CREATE INDEX idx_tb_pagina_search_vector       ON tb_pagina              USING GIN (search_vector);
CREATE INDEX idx_tb_pagina_revisao_pagina      ON tb_pagina_revisao      (pagina_id, created_at DESC);
CREATE INDEX idx_tb_pagina_anexo_pagina        ON tb_pagina_anexo        (pagina_id, created_at DESC);
CREATE INDEX idx_tb_publicacao_cliente_created ON tb_publicacao          (cliente_id, created_at DESC);
CREATE INDEX idx_tb_auditoria_evento_entidade  ON tb_auditoria_evento    (entidade, entidade_id);
CREATE INDEX idx_tb_preview_token_cliente      ON tb_preview_token       (cliente_id);
CREATE INDEX idx_tb_publicacao_changelog_pub   ON tb_publicacao_changelog (publicacao_id, created_at DESC);
