-- =============================================================================
-- RELEASE ORCHESTRATOR
-- produtos RH, releases, itens, histórico e templates
-- =============================================================================

CREATE TABLE tb_produto_rh (
  id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  nome           VARCHAR(200) NOT NULL,
  sigla          VARCHAR(20)  NOT NULL UNIQUE,
  descricao      VARCHAR(500),
  cor            VARCHAR(20)  NOT NULL DEFAULT '#2563eb',
  responsavel_id UUID,
  ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by     VARCHAR(120),
  updated_by     VARCHAR(120)
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_release (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  produto_id      UUID         NOT NULL REFERENCES tb_produto_rh(id),
  versao          VARCHAR(50)  NOT NULL,
  titulo          VARCHAR(200) NOT NULL,
  tipo            VARCHAR(30)  NOT NULL,
  status          VARCHAR(30)  NOT NULL DEFAULT 'RASCUNHO',
  data_prevista   DATE,
  data_publicacao DATE,
  publicado_por   VARCHAR(120),
  responsavel_id  UUID,
  resumo          TEXT,
  observacoes     TEXT,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      VARCHAR(120),
  updated_by      VARCHAR(120),
  CONSTRAINT uq_tb_release_produto_versao UNIQUE (produto_id, versao)
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_release_item (
  id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  release_id     UUID         NOT NULL REFERENCES tb_release(id) ON DELETE CASCADE,
  categoria      VARCHAR(40)  NOT NULL,
  titulo         VARCHAR(300) NOT NULL,
  descricao      TEXT,
  visibilidade   VARCHAR(20)  NOT NULL DEFAULT 'TODOS',
  ordem          INTEGER      NOT NULL DEFAULT 0,
  ticket         VARCHAR(100),
  commit_hash    VARCHAR(100),
  pull_request   VARCHAR(100),
  responsavel_id UUID,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by     VARCHAR(120),
  updated_by     VARCHAR(120)
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_release_historico (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  release_id      UUID         NOT NULL REFERENCES tb_release(id) ON DELETE CASCADE,
  acao            VARCHAR(40)  NOT NULL,
  descricao       VARCHAR(500),
  status_anterior VARCHAR(30),
  status_novo     VARCHAR(30),
  usuario         VARCHAR(120),
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_release_template (
  id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  nome         VARCHAR(200) NOT NULL,
  descricao    VARCHAR(500),
  tipo_release VARCHAR(30),
  produto_id   UUID,
  estrutura    TEXT,
  ativo        BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by   VARCHAR(120),
  updated_by   VARCHAR(120)
);

-- -----------------------------------------------------------------------------
-- Índices
-- -----------------------------------------------------------------------------

CREATE INDEX idx_tb_release_produto          ON tb_release          (produto_id);
CREATE INDEX idx_tb_release_status           ON tb_release          (status);
CREATE INDEX idx_tb_release_updated          ON tb_release          (updated_at DESC);
CREATE INDEX idx_tb_release_item_release     ON tb_release_item     (release_id, ordem);
CREATE INDEX idx_tb_release_historico_release ON tb_release_historico (release_id, created_at DESC);
