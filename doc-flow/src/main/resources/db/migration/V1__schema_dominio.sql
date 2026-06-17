-- =============================================================================
-- DOMÍNIO PRINCIPAL
-- clientes, projetos, módulos, páginas, publicações, vínculos e auditoria
-- =============================================================================

CREATE TABLE tb_cliente (
  id                UUID         PRIMARY KEY,
  nome              VARCHAR(150) NOT NULL,
  slug              VARCHAR(150) NOT NULL UNIQUE,
  ativo             BOOLEAN      NOT NULL DEFAULT TRUE,
  logo_path         VARCHAR(500),
  logo_content_type VARCHAR(100),
  tema_cor_primaria VARCHAR(20),
  tema_cor_fundo    VARCHAR(20),
  created_at        TIMESTAMPTZ  NOT NULL,
  updated_at        TIMESTAMPTZ  NOT NULL,
  created_by        VARCHAR(120),
  updated_by        VARCHAR(120)
);

COMMENT ON COLUMN tb_cliente.tema_cor_primaria IS 'Cor accent do manual (manifest/PWA); ex.: #1a73e8';
COMMENT ON COLUMN tb_cliente.tema_cor_fundo    IS 'Cor de fundo base do tema; ex.: #f8f9fa';

-- -----------------------------------------------------------------------------

CREATE TABLE tb_projeto (
  id         UUID         PRIMARY KEY,
  nome       VARCHAR(150) NOT NULL,
  slug       VARCHAR(150) NOT NULL UNIQUE,
  descricao  TEXT,
  ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ  NOT NULL,
  updated_at TIMESTAMPTZ  NOT NULL,
  created_by VARCHAR(120),
  updated_by VARCHAR(120)
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_modulo (
  id         UUID         PRIMARY KEY,
  projeto_id UUID         NOT NULL REFERENCES tb_projeto(id) ON DELETE CASCADE,
  nome       VARCHAR(150) NOT NULL,
  slug       VARCHAR(150) NOT NULL,
  descricao  TEXT,
  ordem      INT          NOT NULL DEFAULT 0,
  ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ  NOT NULL,
  updated_at TIMESTAMPTZ  NOT NULL,
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  CONSTRAINT uq_tb_modulo_projeto_slug UNIQUE (projeto_id, slug)
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_pagina (
  id            UUID         PRIMARY KEY,
  modulo_id     UUID         NOT NULL REFERENCES tb_modulo(id),
  parent_id     UUID         REFERENCES tb_pagina(id) ON DELETE CASCADE,
  titulo        VARCHAR(200) NOT NULL,
  slug          VARCHAR(200) NOT NULL UNIQUE,
  codigo_tela   VARCHAR(120) NOT NULL UNIQUE,
  resumo        TEXT,
  conteudo_html TEXT,
  status        VARCHAR(30)  NOT NULL,
  ordem         INT          NOT NULL DEFAULT 0,
  ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
  published_at  TIMESTAMPTZ,
  created_at    TIMESTAMPTZ  NOT NULL,
  updated_at    TIMESTAMPTZ  NOT NULL,
  created_by    VARCHAR(120),
  updated_by    VARCHAR(120),
  search_vector TSVECTOR
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_pagina_revisao (
  id            UUID         PRIMARY KEY,
  pagina_id     UUID         NOT NULL REFERENCES tb_pagina(id) ON DELETE CASCADE,
  numero        INT          NOT NULL,
  titulo        VARCHAR(200) NOT NULL,
  slug          VARCHAR(200) NOT NULL,
  codigo_tela   VARCHAR(120) NOT NULL,
  resumo        TEXT,
  conteudo_html TEXT,
  status        VARCHAR(30)  NOT NULL,
  modulo_id     UUID         NOT NULL REFERENCES tb_modulo(id),
  parent_id     UUID         REFERENCES tb_pagina(id),
  created_at    TIMESTAMPTZ  NOT NULL,
  created_by    VARCHAR(120)
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_pagina_anexo (
  id            UUID         PRIMARY KEY,
  pagina_id     UUID         NOT NULL REFERENCES tb_pagina(id) ON DELETE CASCADE,
  nome_original VARCHAR(255) NOT NULL,
  content_type  VARCHAR(120) NOT NULL,
  tamanho_bytes BIGINT       NOT NULL,
  caminho       VARCHAR(700) NOT NULL,
  created_at    TIMESTAMPTZ  NOT NULL,
  updated_at    TIMESTAMPTZ  NOT NULL,
  created_by    VARCHAR(120),
  updated_by    VARCHAR(120)
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_publicacao (
  id                  UUID         PRIMARY KEY,
  cliente_id          UUID         NOT NULL REFERENCES tb_cliente(id),
  versao              VARCHAR(50)  NOT NULL,
  status              VARCHAR(30)  NOT NULL,
  quantidade_paginas  INT          NOT NULL DEFAULT 0,
  quantidade_modulos  INT          NOT NULL DEFAULT 0,
  arquivo_zip_nome    VARCHAR(255),
  arquivo_zip_caminho VARCHAR(500),
  hash_pacote         VARCHAR(120),
  observacao          TEXT,
  relatorio_validacao VARCHAR(4000),
  created_at          TIMESTAMPTZ  NOT NULL,
  updated_at          TIMESTAMPTZ  NOT NULL,
  created_by          VARCHAR(120),
  updated_by          VARCHAR(120)
);

COMMENT ON COLUMN tb_publicacao.relatorio_validacao IS 'JSON com resultado da validação do ZIP (arquivos obrigatórios, etc.)';

-- -----------------------------------------------------------------------------
-- Vínculos N:N
-- -----------------------------------------------------------------------------

CREATE TABLE tb_cliente_projeto (
  id         UUID PRIMARY KEY,
  cliente_id UUID NOT NULL REFERENCES tb_cliente(id) ON DELETE CASCADE,
  projeto_id UUID NOT NULL REFERENCES tb_projeto(id) ON DELETE CASCADE,
  CONSTRAINT uq_tb_cliente_projeto UNIQUE (cliente_id, projeto_id)
);

CREATE TABLE tb_cliente_modulo (
  id         UUID PRIMARY KEY,
  cliente_id UUID NOT NULL REFERENCES tb_cliente(id) ON DELETE CASCADE,
  modulo_id  UUID NOT NULL REFERENCES tb_modulo(id)  ON DELETE CASCADE,
  CONSTRAINT uq_tb_cliente_modulo UNIQUE (cliente_id, modulo_id)
);

CREATE TABLE tb_cliente_pagina (
  id         UUID PRIMARY KEY,
  cliente_id UUID NOT NULL REFERENCES tb_cliente(id) ON DELETE CASCADE,
  pagina_id  UUID NOT NULL REFERENCES tb_pagina(id)  ON DELETE CASCADE,
  CONSTRAINT uq_tb_cliente_pagina UNIQUE (cliente_id, pagina_id)
);

-- -----------------------------------------------------------------------------
-- Auditoria de eventos do sistema
-- -----------------------------------------------------------------------------

CREATE TABLE tb_auditoria_evento (
  id          UUID        PRIMARY KEY,
  entidade    VARCHAR(80) NOT NULL,
  entidade_id UUID,
  acao        VARCHAR(80) NOT NULL,
  descricao   TEXT,
  created_at  TIMESTAMPTZ NOT NULL,
  created_by  VARCHAR(120)
);

-- -----------------------------------------------------------------------------
-- Índices
-- -----------------------------------------------------------------------------

CREATE INDEX idx_tb_modulo_projeto             ON tb_modulo            (projeto_id);
CREATE INDEX idx_tb_pagina_modulo_status       ON tb_pagina            (modulo_id, status);
CREATE INDEX idx_tb_pagina_parent_ordem        ON tb_pagina            (parent_id, ordem);
CREATE INDEX idx_tb_pagina_search_vector       ON tb_pagina            USING GIN (search_vector);
CREATE INDEX idx_tb_pagina_revisao_pagina      ON tb_pagina_revisao    (pagina_id, created_at DESC);
CREATE INDEX idx_tb_pagina_anexo_pagina        ON tb_pagina_anexo      (pagina_id, created_at DESC);
CREATE INDEX idx_tb_publicacao_cliente_created ON tb_publicacao        (cliente_id, created_at DESC);
CREATE INDEX idx_tb_auditoria_evento_entidade  ON tb_auditoria_evento  (entidade, entidade_id);
