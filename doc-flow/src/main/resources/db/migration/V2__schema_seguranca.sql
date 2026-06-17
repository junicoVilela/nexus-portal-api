-- =============================================================================
-- SEGURANÇA
-- usuários, tokens de preview e changelog de publicações
-- =============================================================================

CREATE TABLE tb_usuario (
  id         UUID         PRIMARY KEY,
  username   VARCHAR(80)  NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,
  nome       VARCHAR(150),
  email      VARCHAR(200),
  roles      VARCHAR(200) NOT NULL DEFAULT 'EDITOR',
  ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ  NOT NULL,
  updated_at TIMESTAMPTZ  NOT NULL,
  created_by VARCHAR(120),
  updated_by VARCHAR(120)
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_preview_token (
  id         UUID         PRIMARY KEY,
  cliente_id UUID         NOT NULL,
  token      VARCHAR(120) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ  NOT NULL,
  ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ  NOT NULL,
  created_by VARCHAR(120)
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_publicacao_changelog (
  id            UUID         PRIMARY KEY,
  publicacao_id UUID         NOT NULL REFERENCES tb_publicacao(id) ON DELETE CASCADE,
  pagina_id     UUID,
  pagina_titulo VARCHAR(200) NOT NULL,
  tipo_mudanca  VARCHAR(30)  NOT NULL,
  created_at    TIMESTAMPTZ  NOT NULL
);

-- -----------------------------------------------------------------------------
-- Índices
-- -----------------------------------------------------------------------------

CREATE INDEX idx_tb_preview_token_token         ON tb_preview_token        (token);
CREATE INDEX idx_tb_preview_token_cliente       ON tb_preview_token        (cliente_id);
CREATE INDEX idx_tb_publicacao_changelog_pub    ON tb_publicacao_changelog (publicacao_id, created_at DESC);
